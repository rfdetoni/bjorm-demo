# Optimizing the BJORM demo image

The build pins BJORM to a commit, caches Maven dependencies across rebuilds and produces a Java 25 JRE-only non-root runtime (UID 10001). `slim xray` can inspect the result: `bash scripts/slim-audit.sh bjorm-demo:dev`.

Do not prune Java runtime modules and native PostgreSQL/JDBC dependencies automatically: verify CRUD, schema migration, connection pooling, transactions, warm-up, and benchmark loads on any candidate. No image size or speed reduction is claimed without measurements.
