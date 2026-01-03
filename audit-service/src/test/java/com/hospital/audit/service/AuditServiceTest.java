package com.hospital.audit.service;

import com.hospital.audit.dto.AuditLogDto;
import com.hospital.audit.entity.AuditLog;
import com.hospital.audit.repository.AuditLogRepository;
import com.hospital.common.rbac.service.PermissionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditService.
 */
@QuarkusTest
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Inject
    AuditService auditService;

    @InjectMock
    AuditLogRepository auditLogRepository;

    @InjectMock
    PermissionService permissionService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Default: permission granted
        when(permissionService.requirePermission(anyString()))
            .thenReturn(Uni.createFrom().voidItem());
    }

    private AuditLog createAuditLog(String action, String resourceType) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setTenantId(TENANT_ID);
        log.setUserId(USER_ID);
        log.setUserEmail("user@example.com");
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(UUID.randomUUID());
        log.setDescription("Test audit log");
        log.setTimestamp(LocalDateTime.now());
        return log;
    }

    @Nested
    @DisplayName("Get Audit Logs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("returns paginated audit logs")
        void getAuditLogs_ReturnsPaginatedLogs() {
            // Arrange
            List<AuditLog> logs = List.of(
                createAuditLog("CREATE", "USER"),
                createAuditLog("UPDATE", "PATIENT")
            );
            when(auditLogRepository.findByTenantPaginated(TENANT_ID, 0, 10))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getAuditLogs(TENANT_ID, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAction()).isEqualTo("CREATE");
            assertThat(result.get(1).getAction()).isEqualTo("UPDATE");
        }

        @Test
        @DisplayName("returns empty list when no logs")
        void getAuditLogs_ReturnsEmptyList() {
            // Arrange
            when(auditLogRepository.findByTenantPaginated(TENANT_ID, 0, 10))
                .thenReturn(Uni.createFrom().item(List.of()));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getAuditLogs(TENANT_ID, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Audit Logs By User")
    class GetAuditLogsByUserTests {

        @Test
        @DisplayName("returns logs for specific user")
        void getAuditLogsByUser_ReturnsUserLogs() {
            // Arrange
            List<AuditLog> logs = List.of(createAuditLog("LOGIN", "USER"));
            when(auditLogRepository.findByUser(TENANT_ID, USER_ID, 0, 10))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getAuditLogsByUser(TENANT_ID, USER_ID, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Get Resource History")
    class GetResourceHistoryTests {

        @Test
        @DisplayName("returns history for specific resource")
        void getResourceHistory_ReturnsResourceLogs() {
            // Arrange
            UUID resourceId = UUID.randomUUID();
            List<AuditLog> logs = List.of(
                createAuditLog("CREATE", "PATIENT"),
                createAuditLog("UPDATE", "PATIENT")
            );
            when(auditLogRepository.findByResource(TENANT_ID, "PATIENT", resourceId))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getResourceHistory(TENANT_ID, "PATIENT", resourceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AuditLogDto::getResourceType)
                .containsOnly("PATIENT");
        }
    }

    @Nested
    @DisplayName("Get Audit Logs By Action")
    class GetAuditLogsByActionTests {

        @Test
        @DisplayName("filters logs by action type")
        void getAuditLogsByAction_FiltersCorrectly() {
            // Arrange
            List<AuditLog> logs = List.of(createAuditLog("DELETE", "USER"));
            when(auditLogRepository.findByAction(TENANT_ID, "DELETE", 0, 10))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getAuditLogsByAction(TENANT_ID, "DELETE", 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAction()).isEqualTo("DELETE");
        }
    }

    @Nested
    @DisplayName("Get Recent Audit Logs")
    class GetRecentAuditLogsTests {

        @Test
        @DisplayName("returns limited recent logs")
        void getRecentAuditLogs_ReturnsLimitedLogs() {
            // Arrange
            List<AuditLog> logs = List.of(
                createAuditLog("CREATE", "USER"),
                createAuditLog("LOGIN", "USER")
            );
            when(auditLogRepository.findRecent(TENANT_ID, 10))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getRecentAuditLogs(TENANT_ID, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Failed Operations")
    class GetFailedOperationsTests {

        @Test
        @DisplayName("returns only failed operations")
        void getFailedOperations_ReturnsFailedOps() {
            // Arrange
            AuditLog failedLog = createAuditLog("CREATE", "USER");
            failedLog.setStatusCode(500);
            List<AuditLog> logs = List.of(failedLog);
            when(auditLogRepository.findFailedOperations(TENANT_ID, 0, 10))
                .thenReturn(Uni.createFrom().item(logs));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .getFailedOperations(TENANT_ID, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatusCode()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Search Audit Logs")
    class SearchAuditLogsTests {

        @Test
        @DisplayName("searches logs by description")
        void searchAuditLogs_SearchesByDescription() {
            // Arrange
            AuditLog log = createAuditLog("UPDATE", "PATIENT");
            log.setDescription("Updated patient John Doe");
            when(auditLogRepository.searchByDescription(TENANT_ID, "John", 0, 10))
                .thenReturn(Uni.createFrom().item(List.of(log)));

            // Act
            UniAssertSubscriber<List<AuditLogDto>> subscriber = auditService
                .searchAuditLogs(TENANT_ID, "John", 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            List<AuditLogDto> result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).contains("John");
        }
    }

    @Nested
    @DisplayName("Get Audit Statistics")
    class GetAuditStatisticsTests {

        @Test
        @DisplayName("returns aggregated statistics")
        void getAuditStatistics_ReturnsStats() {
            // Arrange
            when(auditLogRepository.countByTenant(TENANT_ID))
                .thenReturn(Uni.createFrom().item(100L));
            when(auditLogRepository.countByAction(TENANT_ID, "CREATE"))
                .thenReturn(Uni.createFrom().item(40L));
            when(auditLogRepository.countByAction(TENANT_ID, "UPDATE"))
                .thenReturn(Uni.createFrom().item(35L));
            when(auditLogRepository.countByAction(TENANT_ID, "DELETE"))
                .thenReturn(Uni.createFrom().item(10L));
            when(auditLogRepository.findFailedOperations(TENANT_ID, 0, 1))
                .thenReturn(Uni.createFrom().item(List.of(createAuditLog("ERROR", "USER"))));

            // Act
            UniAssertSubscriber<AuditService.AuditStatistics> subscriber = auditService
                .getAuditStatistics(TENANT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            AuditService.AuditStatistics stats = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(stats.total).isEqualTo(100L);
            assertThat(stats.creates).isEqualTo(40L);
            assertThat(stats.updates).isEqualTo(35L);
            assertThat(stats.deletes).isEqualTo(10L);
            assertThat(stats.failed).isEqualTo(1L);
        }
    }
}
