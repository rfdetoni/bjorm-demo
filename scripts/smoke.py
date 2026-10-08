#!/usr/bin/env python3
"""Real HTTP/PostgreSQL acceptance test; run against a disposable demo database."""
import json
import os
import time
import urllib.error
import urllib.request

base = os.getenv("DEMO_BASE_URL", "http://localhost:8080")


def request(method, path, payload=None, expected=200):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(base + path, data=body, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            code, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        code, raw = error.code, error.read()
    assert code == expected, f"{method} {path}: expected {expected}, received {code}: {raw[:500]!r}"
    return json.loads(raw) if raw and raw.startswith((b"{", b"[")) else raw.decode("utf-8")


for attempt in range(75):
    try:
        page = request("GET", "/")
        assert "BJORM" in page
        break
    except (OSError, AssertionError):
        if attempt == 74:
            raise
        time.sleep(2)

before = request("GET", "/api/products?size=10")["total"]
product = {"name": "Teste ponta a ponta", "description": "PostgreSQL + BJORM",
           "price": 29.9, "stock": 12, "active": True}
created = request("POST", "/api/products", product, 201)
assert created["version"] == 0
pid = created["id"]
assert request("GET", f"/api/products/{pid}")["id"] == pid
assert request("GET", "/api/products?search=Teste&size=10")["total"] >= 1
compact = request("GET", "/api/products?compact=true&size=10")
assert compact["items"]
assert "description" not in compact["items"][0], "Projection unexpectedly fetched unused columns"
assert request("GET", "/api/products?size=0", expected=400)
update = {**product, "name": "Atualizado", "version": 0}
updated = request("PUT", f"/api/products/{pid}", update)
assert updated["version"] == 1 and updated["name"] == "Atualizado"
request("PUT", f"/api/products/{pid}", update, 409)
request("DELETE", f"/api/products/{pid}?version=0", expected=409)
request("DELETE", f"/api/products/{pid}?version=1", expected=204)
request("GET", f"/api/products/{pid}", expected=404)
assert request("GET", "/api/products")["total"] == before
assert request("POST", "/api/products/seed?count=30")["inserted"] == 30
page = request("GET", "/api/products?page=1&size=10")
assert len(page["items"]) == 10 and page["total"] >= 30
prev = page["total"]
benchmark = request("POST", "/api/benchmark", {"iterations": 10, "warmup": 1, "concurrency": 1})
assert [x["engine"] for x in benchmark] == ["BJORM", "JDBC"]
assert all(x["transactions"] == 10 and x["p95Ms"] >= 0 for x in benchmark)
assert request("GET", "/api/products")["total"] == prev, "Benchmarks must not leave any rows"
print("PASS: HTML, CREATE, GET, PAGE, FIELDS, UPDATE, OPTIMISTIC LOCK, DELETE, BATCH, BJORM/JDBC TRANSACTIONS")
