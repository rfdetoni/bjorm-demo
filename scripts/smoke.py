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
assert int(created["id"][14], 16) == 7, "Generated product @Id must use UUID v7"
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
# Spring Data Pageable -> BJORM Page and Slice; only Page counts rows.
pageable = request("GET", "/api/products/page?page=0&size=5&sort=name,asc")
assert len(pageable["content"]) == 5 and pageable["totalElements"] >= 30
assert pageable["totalPages"] >= 6
slice1 = request("GET", "/api/products/slice?page=0&size=5&sort=name,asc")
assert len(slice1["content"]) == 5 and slice1["hasNext"] is True
slice2 = request("GET", "/api/products/slice?page=1&size=5&sort=name,asc")
assert len(slice2["content"]) == 5
assert request("GET", "/api/products/page?sort=notMapped,asc", expected=400)
assert request("GET", "/api/products/slice?sort=notMapped,asc", expected=400)
benchmark = request("POST", "/api/benchmark", {"iterations": 10, "warmup": 1, "concurrency": 1})
assert [x["engine"] for x in benchmark] == ["BJORM", "JDBC"]
assert all(x["transactions"] == 10 and x["p95Ms"] >= 0 for x in benchmark)
# Values that used to fail validation must now be accepted.
large_iterations = request("POST", "/api/benchmark", {"iterations": 501, "warmup": 0, "concurrency": 1})
assert all(x["transactions"] == 501 for x in large_iterations)
large_warmup = request("POST", "/api/benchmark", {"iterations": 1, "warmup": 101, "concurrency": 1})
assert all(x["transactions"] == 1 for x in large_warmup)
more_workers = request("POST", "/api/benchmark", {"iterations": 1, "warmup": 0, "concurrency": 9})
assert all(x["transactions"] == 9 for x in more_workers)
assert request("POST", "/api/benchmark", {"iterations": 0, "warmup": 0, "concurrency": 1}, 400)
assert request("GET", "/api/products")["total"] == prev, "Benchmarks must not leave any rows"
order = request("POST", "/api/orders", {
    "customer": "Escola exemplo", "lines": [{"sku": "MAT-001", "quantity": 2}]
}, 201)
assert order["id"] and order["lines"][0]["orderId"] == order["id"]
assert int(order["id"][14], 16) == 7 and int(order["lines"][0]["id"][14], 16) == 7
order_id = order["id"]
line_id = order["lines"][0]["id"]
assert len(request("GET", f"/api/orders/{order_id}")["lines"]) == 1
updated = request("PUT", f"/api/orders/{order_id}/upsert", {
    "customer": "Escola atualizada", "lines": [{"id": line_id, "sku": "MAT-002", "quantity": 3}]
})
assert updated["customer"] == "Escola atualizada" and updated["lines"][0]["sku"] == "MAT-002"
request("DELETE", f"/api/orders/{order_id}", expected=204)
request("GET", f"/api/orders/{order_id}", expected=404)
print("PASS: CRUD, pagination, benchmark, UUID v7, upsert and nested cascade")
