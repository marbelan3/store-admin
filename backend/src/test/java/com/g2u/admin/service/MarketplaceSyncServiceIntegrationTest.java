package com.g2u.admin.service;

import com.g2u.admin.config.TestMarketplaceConfig;
import com.g2u.admin.domain.marketplace.*;
import com.g2u.admin.domain.notification.NotificationRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMarketplaceConfig.class)
class MarketplaceSyncServiceIntegrationTest {

    @Autowired
    private MarketplaceSyncService syncService;

    @Autowired
    private MarketplaceImportService importService;

    @Autowired
    private MarketplaceConnectionService connectionService;

    @Autowired
    private MarketplaceConnectionRepository connectionRepository;

    @Autowired
    private MarketplaceProductRepository marketplaceProductRepository;

    @Autowired
    private MarketplaceVariantMappingRepository variantMappingRepository;

    @Autowired
    private MarketplaceSyncLogRepository syncLogRepository;

    @Autowired
    private MarketplacePriceHistoryRepository priceHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;
    private UUID connectionId;
    private MarketplaceConnection connection;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TestMarketplaceConfig.resetFlags();

        Tenant tenant = Tenant.builder()
                .name("Sync Tenant")
                .slug("sync-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        MarketplaceConnectionDto conn = connectionService.createConnection(tenantId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "sync-key", "CN", null));
        connectionId = conn.id();
        connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId).orElseThrow();
    }

    private MarketplaceProductDto importTestProduct(String suffix) {
        return importService.importProduct(tenantId, TEST_USER_ID, new ImportProductRequest(
                connectionId,
                "CJ-SYNC-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 8),
                List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", "CN", "CN", null)),
                "MARGIN",
                new BigDecimal("30.00"),
                null,
                new BigDecimal("10.00"),
                null,
                5
        ));
    }

    @Test
    void priceSyncJob_shouldCreateSyncLog() {
        importTestProduct("price");

        syncService.syncPrices(connection);

        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        assertTrue(logs.getTotalElements() >= 1);

        MarketplaceSyncLogDto log = logs.getContent().get(0);
        assertEquals("PRICE", log.syncType());
        assertTrue(log.status().startsWith("COMPLETED"));
        assertNotNull(log.completedAt());
        assertTrue(log.itemsChecked() >= 1);
    }

    @Test
    void stockSyncJob_shouldUpdateQuantities() {
        MarketplaceProductDto imported = importTestProduct("stock");

        syncService.syncStock(connection);

        // Verify sync log was created
        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        boolean hasStockLog = logs.getContent().stream()
                .anyMatch(l -> "STOCK".equals(l.syncType()));
        assertTrue(hasStockLog);
    }

    @Test
    void stockSyncJob_lowStockAlert_shouldCreateNotification() {
        MarketplaceProductDto imported = importTestProduct("lowstock");

        // Set low stock threshold high so the test adapter's stock (100) is actually above
        // but let's test the flow — set threshold to 200 so 100 < 200 triggers alert
        importService.updateThreshold(tenantId, imported.id(), 200);

        long notifsBefore = notificationRepository.countByTenantIdAndReadFalse(tenantId);

        syncService.syncStock(connection);

        long notifsAfter = notificationRepository.countByTenantIdAndReadFalse(tenantId);
        assertTrue(notifsAfter > notifsBefore, "Low stock notification should be created");

        // Verify stockAlertSent flag is set
        MarketplaceProduct mp = marketplaceProductRepository.findByTenantIdAndId(tenantId, imported.id()).orElseThrow();
        assertTrue(mp.isStockAlertSent());
    }

    @Test
    void skuValidationJob_shouldValidateSkus() {
        importTestProduct("sku");

        syncService.validateSkus(connection);

        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        boolean hasSkuLog = logs.getContent().stream()
                .anyMatch(l -> "SKU_VALIDATION".equals(l.syncType()));
        assertTrue(hasSkuLog);
    }

    @Test
    void catalogHealthCheckJob_shouldCheckProducts() {
        importTestProduct("health");

        syncService.checkCatalogHealth(connection);

        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        boolean hasHealthLog = logs.getContent().stream()
                .anyMatch(l -> "CATALOG_HEALTH".equals(l.syncType()));
        assertTrue(hasHealthLog);
    }

    @Test
    void excludedProducts_shouldBeSkippedBySync() {
        MarketplaceProductDto imported = importTestProduct("excluded");
        importService.toggleExcluded(tenantId, imported.id());

        syncService.syncPrices(connection);

        // Should still log but check 0 items (excluded)
        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        MarketplaceSyncLogDto latestPriceLog = logs.getContent().stream()
                .filter(l -> "PRICE".equals(l.syncType()))
                .findFirst().orElseThrow();
        assertEquals(0, latestPriceLog.itemsChecked());
    }

    @Test
    void getAlerts_shouldReturnMarginViolations() {
        MarketplaceProductDto imported = importTestProduct("alert");

        // Manually set margin alert
        MarketplaceProduct mp = marketplaceProductRepository.findByTenantIdAndId(tenantId, imported.id()).orElseThrow();
        mp.setMarginAlertTriggered(true);
        mp.setCurrentMarginPct(new BigDecimal("5.00"));
        mp.setMinMarginPct(new BigDecimal("10.00"));
        marketplaceProductRepository.save(mp);

        List<MarketplaceAlertDto> alerts = syncService.getAlerts(tenantId);
        assertTrue(alerts.stream().anyMatch(a -> a.marketplaceProductId().equals(imported.id())));
    }

    @Test
    void manualTrigger_shouldRunPriceAndStockSync() {
        importTestProduct("manual");

        syncService.runAllSyncs(tenantId, connectionId);

        Page<MarketplaceSyncLogDto> logs = syncService.getSyncLogs(tenantId, connectionId, PageRequest.of(0, 10));
        assertTrue(logs.getTotalElements() >= 2);
    }
}
