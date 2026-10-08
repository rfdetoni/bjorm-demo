package com.github.rfdetoni.bjorm.demo.api;

import jakarta.validation.constraints.Min;

/** iterations is per worker; each measured iteration is one complete transaction. */
public record BenchmarkRequest(
    @Min(1) long iterations,
    @Min(0) long warmup,
    @Min(1) int concurrency
) {}
