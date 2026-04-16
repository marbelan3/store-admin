package com.g2u.admin.service;

import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.AuditLogDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Audit Tenant A")
                .slug("audit-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Audit Tenant B")
                .slug("audit-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void logAndQuery_shouldPersistAndReturnAuditEntry() {
        UUID entityId = UUID.randomUUID();
        Map<String, Object> changes = Map.of("name", "New Name", "price", 99.99);

        auditService.doLog(tenantAId, TEST_USER_ID, "CREATE", "Product", entityId, changes);

        Page<AuditLogDto> logs = auditService.getAuditLogs(
                tenantAId, null, null, null, null, null, PageRequest.of(0, 20));

        assertFalse(logs.isEmpty());
        AuditLogDto entry = logs.getContent().stream()
                .filter(l -> l.entityId().equals(entityId))
                .findFirst()
                .orElse(null);
        assertNotNull(entry, "Expected audit log entry not found for entityId=" + entityId);

        assertEquals("CREATE", entry.action());
        assertEquals("Product", entry.entityType());
        assertEquals(entityId, entry.entityId());
        assertEquals(tenantAId, entry.tenantId());
        assertEquals(TEST_USER_ID, entry.userId());
        assertNotNull(entry.changes());
        assertNotNull(entry.createdAt());
    }

    @Test
    void filterByEntityType_shouldReturnOnlyMatchingEntries() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        auditService.doLog(tenantAId, TEST_USER_ID, "CREATE", "Product", productId, Map.of());
        auditService.doLog(tenantAId, TEST_USER_ID, "CREATE", "Category", categoryId, Map.of());
        auditService.doLog(tenantAId, TEST_USER_ID, "UPDATE", "Product", productId, Map.of("status", "ACTIVE"));

        Page<AuditLogDto> productLogs = auditService.getAuditLogs(
                tenantAId, "Product", null, null, null, null, PageRequest.of(0, 20));
        Page<AuditLogDto> categoryLogs = auditService.getAuditLogs(
                tenantAId, "Category", null, null, null, null, PageRequest.of(0, 20));

        assertTrue(productLogs.getTotalElements() >= 2,
                "Expected at least 2 Product audit entries, got " + productLogs.getTotalElements());
        assertTrue(categoryLogs.getTotalElements() >= 1,
                "Expected at least 1 Category audit entry, got " + categoryLogs.getTotalElements());

        productLogs.getContent().forEach(log ->
                assertEquals("Product", log.entityType()));
        categoryLogs.getContent().forEach(log ->
                assertEquals("Category", log.entityType()));
    }

    @Test
    void pagination_shouldRespectPageSize() {
        for (int i = 0; i < 5; i++) {
            auditService.doLog(tenantAId, TEST_USER_ID, "CREATE", "Product",
                    UUID.randomUUID(), Map.of("index", i));
        }

        Page<AuditLogDto> firstPage = auditService.getAuditLogs(
                tenantAId, "Product", null, null, null, null, PageRequest.of(0, 2));
        Page<AuditLogDto> secondPage = auditService.getAuditLogs(
                tenantAId, "Product", null, null, null, null, PageRequest.of(1, 2));

        assertEquals(2, firstPage.getSize());
        assertEquals(2, firstPage.getNumberOfElements());
        assertTrue(firstPage.getTotalElements() >= 5,
                "Expected at least 5 total elements, got " + firstPage.getTotalElements());
        assertTrue(firstPage.getTotalPages() >= 3,
                "Expected at least 3 pages, got " + firstPage.getTotalPages());
        assertEquals(2, secondPage.getNumberOfElements());

        // Pages should contain different entries
        assertNotEquals(
                firstPage.getContent().get(0).id(),
                secondPage.getContent().get(0).id()
        );
    }

    @Test
    void tenantIsolation_auditLogsNotVisibleAcrossTenants() {
        UUID entityIdA = UUID.randomUUID();
        UUID entityIdB = UUID.randomUUID();

        auditService.doLog(tenantAId, TEST_USER_ID, "CREATE", "Product", entityIdA, Map.of("tenant", "A"));
        auditService.doLog(tenantAId, TEST_USER_ID, "UPDATE", "Product", entityIdA, Map.of("tenant", "A"));
        auditService.doLog(tenantBId, TEST_USER_ID, "CREATE", "Product", entityIdB, Map.of("tenant", "B"));

        Page<AuditLogDto> logsA = auditService.getAuditLogs(
                tenantAId, null, null, null, null, null, PageRequest.of(0, 100));
        Page<AuditLogDto> logsB = auditService.getAuditLogs(
                tenantBId, null, null, null, null, null, PageRequest.of(0, 100));

        // Tenant A should not see tenant B entries and vice versa
        logsA.getContent().forEach(log ->
                assertEquals(tenantAId, log.tenantId(),
                        "Tenant A query returned log from different tenant"));
        logsB.getContent().forEach(log ->
                assertEquals(tenantBId, log.tenantId(),
                        "Tenant B query returned log from different tenant"));

        // Tenant A should have at least 2 entries, tenant B at least 1
        assertTrue(logsA.getTotalElements() >= 2,
                "Expected at least 2 audit logs for tenant A, got " + logsA.getTotalElements());
        assertTrue(logsB.getTotalElements() >= 1,
                "Expected at least 1 audit log for tenant B, got " + logsB.getTotalElements());
    }
}
