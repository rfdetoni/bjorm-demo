package com.github.rfdetoni.bjorm.demo.service;

import com.github.rfdetoni.bjorm.Bjorm;
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
 * A/B comparison of three equal statements per transaction (INSERT, SELECT, DELETE).
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
        // Run sequentially: never pit BJORM and JDBC against each other simultaneously.
        return List.of(runVariant("BJORM", request), runVariant("JDBC", request));
    }

    private BenchmarkResult runVariant(String engine, BenchmarkRequest config) {
        boolean bjorm = engine.equals("BJORM");
        for (int i = 0; i < config.warmup(); i++) operation(bjorm, sample());
        List<Future<long[]>> futures = new ArrayList<>();
        CountDownLatch startGate = new CountDownLatch(1);
        long started;
        try (ExecutorService workers = Executors.newFixedThreadPool(config.concurrency())) {
            for (int i = 0; i < config.concurrency(); i++) {
                futures.add(workers.submit(() -> {
                    long[] times = new long[config.iterations()];
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
            started = System.nanoTime();
            startGate.countDown();
            long[] measurements = new long[config.iterations() * config.concurrency()];
            int position = 0;
            try {
                for (var future : futures) {
                    long[] result = future.get();
                    System.arraycopy(result, 0, measurements, position, result.length);
                    position += result.length;
                }
            } catch (ExecutionException e) {
                futures.forEach(f -> f.cancel(true));
                throw new IllegalStateException("Falha no benchmark " + engine, e.getCause());
            } catch (InterruptedException e) {
                futures.forEach(f -> f.cancel(true));
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Benchmark interrompido", e);
            }
            long elapsed = System.nanoTime() - started;
            Arrays.sort(measurements);
            return result(engine, measurements, elapsed);
        }
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
                        if (!rs.next() || !p.id().equals(rs.getObject(1, UUID.class))) throw new SQLException("Readback failure");
                        // Read the same seven columns that BJORM's generated mapper reads.
                        rs.getString(2); rs.getString(3); rs.getBigDecimal(4);
                        rs.getInt(5); rs.getBoolean(6); rs.getInt(7);
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
