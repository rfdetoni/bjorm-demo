package com.github.rfdetoni.bjorm.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSpringDataWebSupport
public class BjormDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(BjormDemoApplication.class, args);
    }
}
