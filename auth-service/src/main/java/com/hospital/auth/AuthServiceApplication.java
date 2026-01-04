package com.hospital.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Auth Service - HS512 JWT authentication with UUID-based users
 */
@SpringBootApplication(scanBasePackages = {"com.hospital.auth", "com.hospital.common"})
@EnableDiscoveryClient
@EnableR2dbcAuditing
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
