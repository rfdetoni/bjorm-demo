# BJORM Demo

A **full CRUD** demonstration of [BJORM](https://github.com/rfdetoni/bjorm) with **Java 25**, **Spring Boot 4.0.3**, **PostgreSQL 17**, Docker Compose, and a standalone HTML/CSS/JS dashboard at `http://localhost:8080`.

The application intentionally **does not use Hibernate, JPA, Spring Data repositories, or JdbcTemplate** for persistence. Products are persisted through generated BJORM entity mappers and its typed query DSL; the benchmark also includes an equivalent plain-JDBC baseline.

## Run everything with Docker

```sh
docker compose up --build
```

Open **http://localhost:8080**. The compose stack starts PostgreSQL, waits for a healthy database and then starts Spring Boot. On the first run the application initializes the `products` table and index. PostgreSQL data lives in the `pgdata` named volume.

```sh
docker compose down       # stop without deleting data
docker compose down -v    # reset DB and delete the demo data
```

The Dockerfile compiles BJORM **from the pinned public GitHub commit** `c8b9eaaf652f3b17081a8dac05a57acaf906c3f8` and installs it into the build stage's local Maven repository. This avoids requiring a GitHub Packages personal access token in Docker. The actual demo depends on the official BJORM Maven coordinates `com.github.rfdetoni.bjorm:*:0.3.5-SNAPSHOT`, rather than copying ORM source into the demo.

## Features

- Full REST CRUD with HTTP `201`, `204`, `400`, `404`, `409`; backend validation.
- Optimistic locking via `@Version`: stale update/delete returns HTTP `409 Conflict`.
- Search by product name, typed DSL ordering, server-side pagination (`size=1..100`), selectable compact projections with `fields("id", "name", "price", "stock", "version")`.
- Seed 250 demo products from the UI via `db.batchInsert()`.
- Benchmarks accessible in the HTML UI: compare BJORM with manual JDBC for **INSERT → SELECT → DELETE** in a committed transaction. Select iteration count, worker concurrency (1/2/4/8), warmup, and view mean, p50, p95, p99, max, throughput.
- Both variants use the same database table, SQL statements, transaction isolation defaults and connection pool; each iteration creates and removes a temporary row, leaving the user's data intact.

### Benchmark limitations

The reported figures are **server-side transaction durations**, not browser/network latencies. They include JDBC connection acquisition, prepare/execute, row conversion, and commit. Tests run in ABBA order (BJORM/JDBC/JDBC/BJORM), sequentially rather than competing for the same connections, with per-worker warmup before the measured phase. Creation of the sample object and UUID happens outside the measured interval. Both engines instantiate the same `Product` record, check primitive SQL NULLs and delete the loaded object. Measurements still include PostgreSQL, connection pooling and scheduling overhead, so this is **not a pure mapper microbenchmark**. Samples depend on PostgreSQL cache warmup, CPU contention, Docker overhead, and JVM JIT: perform repeated tests, use repeated runs and JMH/real load tests for performance claims. This demo is for exploration, not proof that BJORM outperforms JDBC.

The benchmark API is deliberately limited (maximum 500 transactions per worker, eight workers and 100 warmup operations), but it is **not authenticated**. Docker Compose binds ports only on `127.0.0.1`; do not expose this project publicly as-is.

## Local development with Java 25 and Maven

You'll need PostgreSQL 17, Java 25 and Maven 3.9+. Install the [BJORM library](https://github.com/rfdetoni/bjorm) at the pinned commit to your local Maven cache first:

```sh
git clone https://github.com/rfdetoni/bjorm.git ../bjorm
(cd ../bjorm && git checkout c8b9eaaf652f3b17081a8dac05a57acaf906c3f8 && mvn -pl bjorm-core,bjorm-processor,bjorm-spring-boot -am -DskipTests install)
docker compose up -d postgres
mvn spring-boot:run
```

Alternatively, use the Maven artifacts from GitHub Packages. GitHub Packages Maven may require token authentication even for public packages; see [GitHub's Maven registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry). Never commit tokens. Docker and CI deliberately build BJORM from public source for reliability.

## API

| Method | Endpoint | Action |
|---|---|---|
| GET | `/` | HTML dashboard |
| GET | `/api/products?page=0&size=20&sort=name&descending=false&search=` | Paginated list |
| GET | `/api/products?compact=true` | Only selected product properties |
| GET | `/api/products/{uuid}` | One product via BJORM `findOne` |
| POST | `/api/products` | Create |
| PUT | `/api/products/{uuid}` | Update with version |
| DELETE | `/api/products/{uuid}?version=0` | Delete with optimistic locking |
| POST | `/api/products/seed?count=250` | Create up to 2000 sample products |
| POST | `/api/benchmark` | Compare BJORM vs JDBC transaction timings |

Product JSON for POST:

```json
{"name":"Material de alfabetização", "description":"Conjunto didático", "price":39.90, "stock":15, "active":true}
```

PUT uses the same fields plus `"version": 0` from the last read, which is incremented after a successful update. Benchmark request:

```json
{"iterations":100,"warmup":20,"concurrency":4}
```

## Tests and versioning

A GitHub Actions workflow compiles BJORM at the pinned commit, builds this demo with Java 25 and launches PostgreSQL 17 to test the running HTTP API, CRUD/paging/locking, and both benchmark paths. For a local disposable database, use `python3 scripts/smoke.py` against a running instance. The smoke test inserts, updates, deletes and seeds records, and **should never run against a production database**.

The demo starts at `0.1.2-SNAPSHOT` and changes to it go to the `main` branch with a version bump. The BJORM library stays versioned independently (`0.3.5-SNAPSHOT` for this revision). Builds are pinned to the corresponding BJORM source commit; when upgrading the library, change the commit in both Dockerfile and workflow and update the Maven version together.

### Maven snapshot/release

Every validated `main` push builds Java 25, runs the PostgreSQL HTTP smoke test, and deploys the demo's `0.1.2-SNAPSHOT` Maven package to `https://maven.pkg.github.com/rfdetoni/bjorm-demo`. Release manually via **Actions → release-demo → Run workflow** with `version=0.1.0` to publish the stable artifact and advance `main` to `0.1.2-SNAPSHOT`. The release workflow needs GitHub Actions write permission to push commits/tags. A GitHub Packages deployment can require repository package permissions; failures are visible in Actions. No secrets are committed.
