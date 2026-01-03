package com.hospital.audit.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditLog entity.
 */
@DisplayName("AuditLog Entity Tests")
class AuditLogTest {

    private static final String TENANT_ID = "test-tenant";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ACTION = "CREATE";
    private static final String RESOURCE_TYPE = "USER";
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Simple constructor sets required fields")
        void simpleConstructor_SetsRequiredFields() {
            AuditLog log = new AuditLog(TENANT_ID, USER_ID, ACTION, RESOURCE_TYPE, RESOURCE_ID);

            assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(log.getUserId()).isEqualTo(USER_ID);
            assertThat(log.getAction()).isEqualTo(ACTION);
            assertThat(log.getResourceType()).isEqualTo(RESOURCE_TYPE);
            assertThat(log.getResourceId()).isEqualTo(RESOURCE_ID);
        }

        @Test
        @DisplayName("Full constructor sets all fields")
        void fullConstructor_SetsAllFields() {
            String userEmail = "user@example.com";
            String description = "Created new user";
            String oldValue = null;
            String newValue = "{\"name\":\"John\"}";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String httpMethod = "POST";
            String requestPath = "/api/users";
            Integer statusCode = 201;

            AuditLog log = new AuditLog(
                TENANT_ID, USER_ID, userEmail, ACTION,
                RESOURCE_TYPE, RESOURCE_ID, description,
                oldValue, newValue, ipAddress,
                userAgent, httpMethod, requestPath, statusCode
            );

            assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(log.getUserId()).isEqualTo(USER_ID);
            assertThat(log.getUserEmail()).isEqualTo(userEmail);
            assertThat(log.getAction()).isEqualTo(ACTION);
            assertThat(log.getResourceType()).isEqualTo(RESOURCE_TYPE);
            assertThat(log.getResourceId()).isEqualTo(RESOURCE_ID);
            assertThat(log.getDescription()).isEqualTo(description);
            assertThat(log.getOldValue()).isNull();
            assertThat(log.getNewValue()).isEqualTo(newValue);
            assertThat(log.getIpAddress()).isEqualTo(ipAddress);
            assertThat(log.getUserAgent()).isEqualTo(userAgent);
            assertThat(log.getHttpMethod()).isEqualTo(httpMethod);
            assertThat(log.getRequestPath()).isEqualTo(requestPath);
            assertThat(log.getStatusCode()).isEqualTo(statusCode);
        }

        @Test
        @DisplayName("Default constructor creates empty object")
        void defaultConstructor_CreatesEmptyObject() {
            AuditLog log = new AuditLog();

            assertThat(log.getTenantId()).isNull();
            assertThat(log.getUserId()).isNull();
            assertThat(log.getAction()).isNull();
            assertThat(log.getResourceType()).isNull();
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("setters update values correctly")
        void setters_UpdateValues() {
            AuditLog log = new AuditLog();

            log.setTenantId(TENANT_ID);
            log.setUserId(USER_ID);
            log.setAction(ACTION);
            log.setResourceType(RESOURCE_TYPE);
            log.setResourceId(RESOURCE_ID);
            log.setStatusCode(200);

            assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(log.getUserId()).isEqualTo(USER_ID);
            assertThat(log.getAction()).isEqualTo(ACTION);
            assertThat(log.getResourceType()).isEqualTo(RESOURCE_TYPE);
            assertThat(log.getResourceId()).isEqualTo(RESOURCE_ID);
            assertThat(log.getStatusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("HTTP Context Tests")
    class HttpContextTests {

        @Test
        @DisplayName("can store HTTP request context")
        void canStoreHttpContext() {
            AuditLog log = new AuditLog();
            log.setHttpMethod("PUT");
            log.setRequestPath("/api/patients/123");
            log.setStatusCode(200);
            log.setIpAddress("10.0.0.1");
            log.setUserAgent("TestClient/1.0");

            assertThat(log.getHttpMethod()).isEqualTo("PUT");
            assertThat(log.getRequestPath()).isEqualTo("/api/patients/123");
            assertThat(log.getStatusCode()).isEqualTo(200);
            assertThat(log.getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(log.getUserAgent()).isEqualTo("TestClient/1.0");
        }
    }

    @Nested
    @DisplayName("Change Tracking Tests")
    class ChangeTrackingTests {

        @Test
        @DisplayName("can store old and new values")
        void canStoreOldAndNewValues() {
            AuditLog log = new AuditLog();
            String oldJson = "{\"name\":\"Old Name\"}";
            String newJson = "{\"name\":\"New Name\"}";

            log.setOldValue(oldJson);
            log.setNewValue(newJson);

            assertThat(log.getOldValue()).isEqualTo(oldJson);
            assertThat(log.getNewValue()).isEqualTo(newJson);
        }

        @Test
        @DisplayName("can handle null old value for creates")
        void canHandleNullOldValueForCreates() {
            AuditLog log = new AuditLog();
            log.setAction("CREATE");
            log.setOldValue(null);
            log.setNewValue("{\"id\":\"123\"}");

            assertThat(log.getOldValue()).isNull();
            assertThat(log.getNewValue()).isNotNull();
        }
    }
}
