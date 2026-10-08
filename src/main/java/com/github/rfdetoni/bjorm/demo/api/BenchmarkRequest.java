package com.github.rfdetoni.bjorm.demo.api;

import jakarta.validation.constraints.*;

/** iterations is per worker; each measured iteration is one complete transaction. */
public record BenchmarkRequest(
    @Min(10) @Max(500) int iterations,
    @Min(0) @Max(100) int warmup,
    @Min(1) @Max(8) int concurrency
) {}
