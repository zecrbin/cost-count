package com.costcount.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI costCountOpenApi() {
        return new OpenAPI().info(new Info().title("Backend API").description("通用后端接口文档").version("0.1.0"));
    }
}
