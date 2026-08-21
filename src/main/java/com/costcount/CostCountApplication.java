package com.costcount;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.costcount.mapper")
@SpringBootApplication
public class CostCountApplication {
    public static void main(String[] args) {
        SpringApplication.run(CostCountApplication.class, args);
    }
}
