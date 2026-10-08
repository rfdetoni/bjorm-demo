package com.github.rfdetoni.bjorm.demo.api;

public record BenchmarkResult(
    String engine, int transactions,
    double avgMs, double p50Ms, double p95Ms, double p99Ms,
    double maxMs, double throughputPerSecond, double wallClockMs
) {}
