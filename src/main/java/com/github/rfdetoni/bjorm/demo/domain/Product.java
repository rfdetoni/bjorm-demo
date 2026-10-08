package com.github.rfdetoni.bjorm.demo.domain;

import com.github.rfdetoni.bjorm.Id;
import com.github.rfdetoni.bjorm.Table;
import com.github.rfdetoni.bjorm.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Table("products")
public record Product(
    @Id UUID id,
    String name,
    String description,
    BigDecimal price,
    int stock,
    boolean active,
    @Version int version
) {}
