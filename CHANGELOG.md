# Changelog

## 0.1.9-SNAPSHOT

- Pin BJORM 0.3.10-SNAPSHOT with JDBC maxRows protection and equalize benchmark statement timeouts between BJORM and handwritten JDBC.
- Keep reproducible Docker/CI BJORM SHA and package versions synchronized.

## 0.1.8-SNAPSHOT

- Upgrade BJORM to 0.3.9-SNAPSHOT with auto eager JOINs and per-instance JDBC query timeout, buffered row budget and fetch size.
- Remove the per-order child SELECT in the REST controller; assert Spring @Transactional commit/rollback.
- Tune PostgreSQL batch inserts with pgJDBC reWriteBatchedInserts.

## 0.1.7-SNAPSHOT

- Fix HTTP smoke assertion to use Spring Data `Slice` JSON property `last` (instead of a nonexistent `hasNext` property).
- Assert count metadata is absent from `Slice`, preserving zero-count pagination semantics.
- No production persistence change; continue to pin BJORM 0.3.8-SNAPSHOT.

## 0.1.6-SNAPSHOT

- Use inferred UUID v7 generation in `@Id` POJOs and immutable product records, without explicitly calling UUID.randomUUID in CRUD or seed paths.
- Persist new immutable Product records through `insertReturning` and seed through `batchInsertReturning`.
- Upgrade to BJORM 0.3.8-SNAPSHOT with type-based IDs; preserve manual benchmark UUID creation for fair comparison.


- Add `/api/products/page` and `/api/products/slice` accepting Spring `Pageable` parameters.
- Verify pagination metadata, navigation and invalid sort property handling with PostgreSQL HTTP smoke checks.
- Use BJORM 0.3.8-SNAPSHOT and update the pinned reproducible library build.


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
