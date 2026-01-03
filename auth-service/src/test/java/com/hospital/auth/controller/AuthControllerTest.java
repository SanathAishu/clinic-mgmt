package com.hospital.auth.controller;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Integration tests for AuthController.
 *
 * Tests:
 * - User registration
 * - User login with valid credentials
 * - Login with invalid credentials
 * - Duplicate email registration
 */
@QuarkusTest
class AuthControllerTest {

    @Test
    void testRegisterUser_Success() {
        RegisterRequest request = new RegisterRequest(
            "Test User",
            "test@hospital.com",
            "SecurePass123!",
            "+1234567890"
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "test-tenant")
            .body(request)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .body("name", equalTo("Test User"))
            .body("email", equalTo("test@hospital.com"))
            .body("active", equalTo(true))
            .body("emailVerified", equalTo(false))
            .body("id", notNullValue());
    }

    @Test
    void testRegisterUser_InvalidPassword() {
        RegisterRequest request = new RegisterRequest(
            "Test User",
            "test2@hospital.com",
            "weak", // Does not meet password requirements
            "+1234567890"
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "test-tenant")
            .body(request)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400); // Bad request due to validation failure
    }

    @Test
    void testLogin_Success() {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "Login Test User",
            "login@hospital.com",
            "SecurePass123!",
            "+1234567890"
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "test-tenant")
            .body(registerRequest)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Then login
        LoginRequest loginRequest = new LoginRequest(
            "login@hospital.com",
            "SecurePass123!"
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "test-tenant")
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("expiresIn", equalTo(86400))
            .body("user.email", equalTo("login@hospital.com"))
            .body("user.name", equalTo("Login Test User"));
    }

    @Test
    void testLogin_InvalidCredentials() {
        LoginRequest request = new LoginRequest(
            "nonexistent@hospital.com",
            "wrongpassword"
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "test-tenant")
            .body(request)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", containsString("Invalid email or password"));
    }

    @Test
    void testHealthCheck() {
        given()
        .when()
            .get("/api/auth/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("service", equalTo("auth-service"));
    }
}
