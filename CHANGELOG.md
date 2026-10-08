# Changelog

## 0.1.5-SNAPSHOT

- Add `/api/products/page` and `/api/products/slice` accepting Spring `Pageable` parameters.
- Verify pagination metadata, navigation and invalid sort property handling with PostgreSQL HTTP smoke checks.
- Use BJORM 0.3.7-SNAPSHOT and update the pinned reproducible library build.


## 0.1.3-SNAPSHOT

- Remove fixed upper bounds for benchmark transactions/worker, warm-up and concurrent workers in the REST API and HTML interface.
- Use bounded-memory HdrHistogram sampling (approximate three-significant-digit latency percentiles) instead of storing every transaction latency.
- Use Java virtual threads for benchmark workers and configurable PostgreSQL connection pool size / connection timeout; retain input sanity checks and ABBA comparison.


## 0.1.1-SNAPSHOT

- Make the benchmark comparison more equivalent: JDBC materializes the same Product record and checks primitive SQL NULLs.
- Alternate benchmark execution in ABBA order, warm up every worker before measurement and aggregate both passes per engine.
- Upgrade the pinned BJORM library to 0.3.4-SNAPSHOT (typed BigDecimal binder).

## 0.1.0-SNAPSHOT

- Initial Java 25, Spring Boot 4 and PostgreSQL application using BJORM generated JDBC mappings.
- CRUD, optimistic locking, paginated/filtered/ordered listing, selected-property queries and batch seeding.
- Responsive HTML dashboard with form, editable table, paging and live request latency.
- Backend transaction timing comparison with manual JDBC (p50/p95/p99/throughput).
- Docker Compose and Java 25 + PostgreSQL integration CI.
