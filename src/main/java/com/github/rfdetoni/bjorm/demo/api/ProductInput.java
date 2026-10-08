package com.github.rfdetoni.bjorm.demo.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductInput(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 800) String description,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Min(0) int stock,
    boolean active,
    @Min(0) Integer version
) {}
