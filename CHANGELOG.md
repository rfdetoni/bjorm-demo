# Changelog

## 0.1.1-SNAPSHOT

- Compare BJORM and JDBC in balanced ABBA order with per-worker warmup and equal total transactions.
- Match JDBC and BJORM record materialization and primitive SQL NULL checks.
- Upgrade to BJORM 0.3.4-SNAPSHOT with direct BigDecimal bindings.

## 0.1.0-SNAPSHOT

- Initial Java 25, Spring Boot 4 and PostgreSQL application using BJORM generated JDBC mappings.
- CRUD, optimistic locking, paginated/filtered/ordered listing, selected-property queries and batch seeding.
- Responsive HTML dashboard with form, editable table, paging and live request latency.
- Backend transaction timing comparison with manual JDBC (p50/p95/p99/throughput).
- Docker Compose and Java 25 + PostgreSQL integration CI.
