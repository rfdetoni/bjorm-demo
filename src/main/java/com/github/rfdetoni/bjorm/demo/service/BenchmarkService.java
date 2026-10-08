package com.github.rfdetoni.bjorm.demo.service;

import com.github.rfdetoni.bjorm.Bjorm;
import com.github.rfdetoni.bjorm.JdbcValues;
import com.github.rfdetoni.bjorm.demo.api.BenchmarkRequest;
import com.github.rfdetoni.bjorm.demo.api.BenchmarkResult;
import com.github.rfdetoni.bjorm.demo.domain.Product;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ABBA comparison of three equal statements per transaction (INSERT, SELECT, DELETE).
 * Samples include connection acquisition, commit, generated mapping and JDBC driver time,
 * but exclude HTTP/network/browser rendering and object creation preceding each operation.
 * This is a bounded demonstration, not a calibrated performance benchmark.
 */
@Service
public class BenchmarkService {
    private static final String INSERT = "INSERT INTO products (id, name, description, price, stock, active, version) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND = "SELECT id, name, description, price, stock, active, version FROM products WHERE id = ?";
    private static final String DELETE = "DELETE FROM products WHERE id = ? AND version = ?";
    private final Bjorm db;
    private final DataSource source;

    public BenchmarkService(Bjorm db, DataSource source) { this.db = db; this.source = source; }

    public List<BenchmarkResult> compare(BenchmarkRequest request) {
        // Run separate, balanced passes to reduce cache/JIT/time-drift bias without pool contention.
        int first = (request.iterations() + 1) / 2;
        int second = request.iterations() / 2;
        Run bjorm1 = runVariant(true, first, request);
        Run jdbc1 = runVariant(false, first, request);
        Run jdbc2 = runVariant(false, second, request);
        Run bjorm2 = runVariant(true, second, request);
        return List.of(aggregate("BJORM", bjorm1, bjorm2), aggregate("JDBC", jdbc1, jdbc2));
    }

    private record Run(long[] measurements, long elapsedNs) {}

    private Run runVariant(boolean bjorm, int iterationsPerWorker, BenchmarkRequest config) {
        List<Future<long[]>> futures = new ArrayList<>();
        CountDownLatch warmupComplete = new CountDownLatch(config.concurrency());
        CountDownLatch startGate = new CountDownLatch(1);
        long started;
        try (ExecutorService workers = Executors.newFixedThreadPool(config.concurrency())) {
            for (int i = 0; i < config.concurrency(); i++) {
                futures.add(workers.submit(() -> {
                    long[] times = new long[iterationsPerWorker];
                    try {
                        for (int j = 0; j < config.warmup(); j++) operation(bjorm, sample());
                    } finally {
                        warmupComplete.countDown();
                    }
                    startGate.await();
                    for (int j = 0; j < times.length; j++) {
                        Product product = sample();
                        long before = System.nanoTime();
                        operation(bjorm, product);
                        times[j] = System.nanoTime() - before;
                    }
                    return times;
                }));
            }
            try {
                warmupComplete.await();
            } catch (InterruptedException e) {
                futures.forEach(f -> f.cancel(true));
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Benchmark interrupted during warmup", e);
            }
            started = System.nanoTime();
            startGate.countDown();
            long[] measurements = new long[iterationsPerWorker * config.concurrency()];
            int position = 0;
            try {
                for (var future : futures) {
                    long[] result = future.get();
                    System.arraycopy(result, 0, measurements, position, result.length);
                    position += result.length;
                }
            } catch (ExecutionException e) {
                futures.forEach(f -> f.cancel(true));
                throw new IllegalStateException("Falha no benchmark " + (bjorm ? "BJORM" : "JDBC"), e.getCause());
            } catch (InterruptedException e) {
                futures.forEach(f -> f.cancel(true));
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Benchmark interrompido", e);
            }
            long elapsed = System.nanoTime() - started;
            return new Run(measurements, elapsed);
        }
    }

    private static BenchmarkResult aggregate(String engine, Run a, Run b) {
        long[] samples = Arrays.copyOf(a.measurements(), a.measurements().length + b.measurements().length);
        System.arraycopy(b.measurements(), 0, samples, a.measurements().length, b.measurements().length);
        Arrays.sort(samples);
        return result(engine, samples, a.elapsedNs() + b.elapsedNs());
    }

    private static Product sample() {
        return new Product(UUID.randomUUID(), "bench-sample", "Temporary benchmark row",
            new BigDecimal("12.50"), 10, true, 0);
    }

    private void operation(boolean bjorm, Product p) {
        if (bjorm) {
            db.tx(tx -> {
                tx.insert(p);
                Product loaded = tx.find(Product.class, p.id());
                if (loaded == null || !p.id().equals(loaded.id())) throw new IllegalStateException("Readback failure");
                if (tx.delete(loaded) != 1) throw new IllegalStateException("Delete failure");
            });
        } else {
            rawJdbcTransaction(p);
        }
    }

    /** Same table, columns, statement order, number of roundtrips and transaction boundary as BJORM. */
    private void rawJdbcTransaction(Product p) {
        try (Connection c = source.getConnection()) {
            boolean oldAutoCommit = c.getAutoCommit();
            if (!oldAutoCommit) throw new IllegalStateException("Benchmark expects auto-commit pool connections");
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(INSERT)) {
                    ps.setObject(1, p.id());
                    ps.setString(2, p.name());
                    ps.setString(3, p.description());
                    ps.setBigDecimal(4, p.price());
                    ps.setInt(5, p.stock());
                    ps.setBoolean(6, p.active());
                    ps.setInt(7, p.version());
                    if (ps.executeUpdate() != 1) throw new SQLException("Insert failure");
                }
                try (PreparedStatement ps = c.prepareStatement(FIND)) {
                    ps.setObject(1, p.id());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Readback failure");
                        // Read and materialize exactly as BJORM's generated mapper does.
                        Product loaded = new Product(rs.getObject(1, UUID.class), rs.getString(2),
                            rs.getString(3), rs.getBigDecimal(4), JdbcValues.requiredInt(rs, 5),
                            JdbcValues.requiredBoolean(rs, 6), JdbcValues.requiredInt(rs, 7));
                        if (!p.id().equals(loaded.id())) throw new SQLException("Readback failure");
                        p = loaded;
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(DELETE)) {
                    ps.setObject(1, p.id());
                    ps.setInt(2, p.version());
                    if (ps.executeUpdate() != 1) throw new SQLException("Delete failure");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                c.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("JDBC benchmark failed", e);
        }
    }

    private static BenchmarkResult result(String engine, long[] ns, long elapsed) {
        double sum = 0;
        for (long n : ns) sum += n;
        int n = ns.length;
        return new BenchmarkResult(engine, n, ms(sum / n), ms(percentile(ns, .50)),
            ms(percentile(ns, .95)), ms(percentile(ns, .99)), ms(ns[n - 1]),
            n / (elapsed / 1_000_000_000.0), ms(elapsed));
    }
    private static long percentile(long[] sorted, double probability) {
        return sorted[Math.max(0, (int) Math.ceil(probability * sorted.length) - 1)];
    }
    private static double ms(double nanoseconds) { return Math.round(nanoseconds / 1000.0) / 1000.0; }
}
