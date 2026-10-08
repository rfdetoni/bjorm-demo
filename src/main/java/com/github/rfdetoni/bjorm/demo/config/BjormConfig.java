package com.github.rfdetoni.bjorm.demo.config;

import com.github.rfdetoni.bjorm.EntityMapper;
import com.github.rfdetoni.bjorm.demo.domain.Product;
import com.github.rfdetoni.bjorm.demo.domain.Product_BjormMapper;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrder;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrder_BjormMapper;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrderLine;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrderLine_BjormMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BjormConfig {
    /** Register every generated entity mapper with the Spring adapter. */
    @Bean EntityMapper<Product> productMapper() { return Product_BjormMapper.INSTANCE; }
    @Bean EntityMapper<DemoOrder> orderMapper() { return DemoOrder_BjormMapper.INSTANCE; }
    @Bean EntityMapper<DemoOrderLine> orderLineMapper() { return DemoOrderLine_BjormMapper.INSTANCE; }
}
