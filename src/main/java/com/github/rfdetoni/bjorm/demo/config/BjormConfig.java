package com.github.rfdetoni.bjorm.demo.config;

import com.github.rfdetoni.bjorm.EntityMapper;
import com.github.rfdetoni.bjorm.demo.domain.Product;
import com.github.rfdetoni.bjorm.demo.domain.Product_BjormMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BjormConfig {
    /** Registers the compile-time generated mapper with BJORM's optional Spring adapter. */
    @Bean
    EntityMapper<Product> productMapper() {
        return Product_BjormMapper.INSTANCE;
    }
}
