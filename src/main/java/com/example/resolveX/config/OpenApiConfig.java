package com.example.resolveX.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI resolveXOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResolveX Incident API")
                        .version("v1")
                        .description("API for creating, retrieving, filtering, sorting, grouping, updating, and deleting incidents.")
                        .license(new License().name("Internal use")));
    }
}
