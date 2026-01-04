package com.hospital.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Hospital Service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${springdoc.api.title:#{null}}")
    private String apiTitle;

    @Value("${springdoc.api.description:#{null}}")
    private String apiDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        String title = apiTitle != null ? apiTitle : formatServiceName(applicationName);
        String description = apiDescription != null ? apiDescription :
            "REST API documentation for " + title;

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version("1.0.0")
                        .description(description)
                        .contact(new Contact()
                                .name("Hospital Management System")
                                .email("support@hospital.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://hospital.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private String formatServiceName(String serviceName) {
        if (serviceName == null) return "Hospital Service API";
        return serviceName
                .replace("-", " ")
                .replace("service", "Service")
                .substring(0, 1).toUpperCase() +
                serviceName.replace("-", " ").substring(1) + " API";
    }
}
