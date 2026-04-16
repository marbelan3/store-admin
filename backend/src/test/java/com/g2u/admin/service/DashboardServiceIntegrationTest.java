package com.g2u.admin.service;

import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.DashboardStatsDto;
import com.g2u.admin.web.dto.RecentProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProductService productService;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Dashboard Tenant A")
                .slug("dash-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Dashboard Tenant B")
                .slug("dash-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void emptyDashboard_allCountsShouldBeZero() {
        DashboardStatsDto stats = dashboardService.getStats(tenantAId);

        assertEquals(0, stats.totalProducts());
        assertEquals(0, stats.activeProducts());
        assertEquals(0, stats.draftProducts());
        assertEquals(0, stats.archivedProducts());
        assertEquals(0, stats.totalCategories());
        assertEquals(0, stats.totalTags());
        assertEquals(0, stats.lowStockCount());
        assertEquals(0, stats.noImagesCount());
        assertEquals(0, stats.uncategorizedCount());
        assertNotNull(stats.productsByStatus());
        assertNotNull(stats.recentlyUpdated());
        assertTrue(stats.recentlyUpdated().isEmpty());
    }

    @Test
    void withProducts_shouldReflectCorrectCounts() {
        createProduct(tenantAId, "Draft One", "DRAFT");
        createProduct(tenantAId, "Draft Two", "DRAFT");
        createProduct(tenantAId, "Active One", "ACTIVE");
        createProduct(tenantAId, "Active Two", "ACTIVE");
        createProduct(tenantAId, "Active Three", "ACTIVE");

        DashboardStatsDto stats = dashboardService.getStats(tenantAId);

        assertEquals(5, stats.totalProducts());
        assertEquals(3, stats.activeProducts());
        assertEquals(2, stats.draftProducts());
        assertEquals(0, stats.archivedProducts());
    }

    @Test
    void productsByStatus_shouldReturnCorrectMap() {
        createProduct(tenantAId, "Draft", "DRAFT");
        createProduct(tenantAId, "Active", "ACTIVE");
        createProduct(tenantAId, "Active 2", "ACTIVE");

        DashboardStatsDto stats = dashboardService.getStats(tenantAId);
        Map<String, Long> byStatus = stats.productsByStatus();

        assertNotNull(byStatus);
        assertEquals(2L, byStatus.get("ACTIVE"));
        assertEquals(1L, byStatus.get("DRAFT"));
        assertEquals(0L, byStatus.get("ARCHIVED"));
    }

    @Test
    void recentlyUpdatedProducts_shouldReturnList() {
        createProduct(tenantAId, "Product Alpha", "DRAFT");
        createProduct(tenantAId, "Product Beta", "ACTIVE");
        createProduct(tenantAId, "Product Gamma", "ACTIVE");

        DashboardStatsDto stats = dashboardService.getStats(tenantAId);
        List<RecentProductDto> recent = stats.recentlyUpdated();

        assertNotNull(recent);
        assertEquals(3, recent.size());
        // Most recently created/updated should appear first
        for (RecentProductDto dto : recent) {
            assertNotNull(dto.id());
            assertNotNull(dto.name());
            assertNotNull(dto.updatedAt());
        }
    }

    @Test
    void tenantIsolation_statsShouldOnlyReflectOwnTenantData() {
        createProduct(tenantAId, "A Product 1", "ACTIVE");
        createProduct(tenantAId, "A Product 2", "DRAFT");
        createProduct(tenantAId, "A Product 3", "ACTIVE");

        createProduct(tenantBId, "B Product 1", "DRAFT");

        DashboardStatsDto statsA = dashboardService.getStats(tenantAId);
        DashboardStatsDto statsB = dashboardService.getStats(tenantBId);

        assertEquals(3, statsA.totalProducts());
        assertEquals(2, statsA.activeProducts());
        assertEquals(1, statsA.draftProducts());
        assertEquals(3, statsA.recentlyUpdated().size());

        assertEquals(1, statsB.totalProducts());
        assertEquals(0, statsB.activeProducts());
        assertEquals(1, statsB.draftProducts());
        assertEquals(1, statsB.recentlyUpdated().size());
    }

    private void createProduct(UUID tenantId, String name, String status) {
        CreateProductRequest request = new CreateProductRequest(
                name, null, null, null,
                status, new BigDecimal("10.00"), null, "UAH",
                null, null, false, 0, null, "kg",
                null, null, null, null, null, null, null
        );
        productService.createProduct(tenantId, TEST_USER_ID, request);
    }
}
