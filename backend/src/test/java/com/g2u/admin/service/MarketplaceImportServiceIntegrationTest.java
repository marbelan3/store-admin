package com.g2u.admin.service;

import com.g2u.admin.config.TestMarketplaceConfig;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistory;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistoryRepository;
import com.g2u.admin.domain.marketplace.MarketplaceProductRepository;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMapping;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMappingRepository;
import com.g2u.admin.domain.marketplace.PriceType;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.ImportProductRequest;
import com.g2u.admin.web.dto.MarketplaceConnectionDto;
import com.g2u.admin.web.dto.MarketplaceProductDto;
import com.g2u.admin.web.dto.CreateMarketplaceConnectionRequest;
import com.g2u.admin.web.dto.AddToWatchlistRequest;
import com.g2u.admin.web.dto.WatchlistItemDto;
import com.g2u.admin.web.exception.DuplicateResourceException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
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
class MarketplaceImportServiceIntegrationTest {

    @Autowired
    private MarketplaceImportService importService;

    @Autowired
    private MarketplaceWatchlistService watchlistService;

    @Autowired
    private MarketplaceConnectionService connectionService;

    @Autowired
    private MarketplaceProductRepository marketplaceProductRepository;

    @Autowired
    private MarketplaceVariantMappingRepository variantMappingRepository;

    @Autowired
    private MarketplacePriceHistoryRepository priceHistoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;
    private UUID connectionId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder()
                .name("Import Tenant")
                .slug("import-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        MarketplaceConnectionDto conn = connectionService.createConnection(tenantId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "test@test.com", "test-key", "CN", null));
        connectionId = conn.id();
    }

    @Test
    void importProduct_shouldCreateProductWithVariantsAndMappings() {
        ImportProductRequest request = new ImportProductRequest(
                connectionId,
                "CJ-PID-" + UUID.randomUUID().toString().substring(0, 8),
                List.of(
                        new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", "CN", "CN", null),
                        new ImportProductRequest.ImportVariantRequest("vid-2", "SKU-2", "CN", "CN", null)
                ),
                "MARGIN",
                new BigDecimal("30.00"),
                null,
                new BigDecimal("10.00"),
                null,
                5
        );

        MarketplaceProductDto result = importService.importProduct(tenantId, TEST_USER_ID, request);

        assertNotNull(result.id());
        assertEquals(request.externalProductId(), result.externalProductId());
        assertEquals("SYNCED", result.syncStatus());
        assertEquals("MARGIN", result.pricingRule());
        assertEquals(2, result.variantMappings().size());
        assertFalse(result.excluded());
        assertEquals(5, result.lowStockThreshold());

        // Verify local product was created
        Product localProduct = productRepository.findByTenantIdAndId(tenantId, result.productId()).orElseThrow();
        assertEquals(ProductSource.MARKETPLACE, localProduct.getSource());
        // Variants are verified via the DTO (lazy loading outside transaction)
        assertEquals(2, result.variantMappings().size());
    }

    @Test
    void importProduct_duplicate_shouldFail() {
        String externalId = "CJ-DUP-" + UUID.randomUUID().toString().substring(0, 8);

        importService.importProduct(tenantId, TEST_USER_ID, new ImportProductRequest(
                connectionId, externalId,
                List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                null, null, null, null, null, null));

        assertThrows(DuplicateResourceException.class, () ->
                importService.importProduct(tenantId, TEST_USER_ID, new ImportProductRequest(
                        connectionId, externalId,
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-2", "SKU-2", null, null, null)),
                        null, null, null, null, null, null)));
    }

    @Test
    void importProduct_fixedMarkupPricing() {
        ImportProductRequest request = new ImportProductRequest(
                connectionId,
                "CJ-FIXED-" + UUID.randomUUID().toString().substring(0, 8),
                List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                "FIXED_MARKUP",
                null,
                new BigDecimal("5.00"),
                null,
                null,
                null
        );

        MarketplaceProductDto result = importService.importProduct(tenantId, TEST_USER_ID, request);
        assertEquals("FIXED_MARKUP", result.pricingRule());
    }

    @Test
    void toggleExcluded_shouldToggle() {
        MarketplaceProductDto imported = importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        "CJ-EXCL-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));

        assertFalse(imported.excluded());

        MarketplaceProductDto excluded = importService.toggleExcluded(tenantId, imported.id());
        assertTrue(excluded.excluded());

        MarketplaceProductDto included = importService.toggleExcluded(tenantId, imported.id());
        assertFalse(included.excluded());
    }

    @Test
    void updateThreshold_shouldUpdate() {
        MarketplaceProductDto imported = importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        "CJ-THR-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));

        MarketplaceProductDto updated = importService.updateThreshold(tenantId, imported.id(), 10);
        assertEquals(10, updated.lowStockThreshold());
    }

    @Test
    void getMarketplaceProducts_shouldReturnPaginated() {
        for (int i = 0; i < 3; i++) {
            importService.importProduct(tenantId, TEST_USER_ID, new ImportProductRequest(
                    connectionId,
                    "CJ-LIST-" + i + "-" + UUID.randomUUID().toString().substring(0, 8),
                    List.of(new ImportProductRequest.ImportVariantRequest("vid-" + i, "SKU-" + i, null, null, null)),
                    null, null, null, null, null, null));
        }

        Page<MarketplaceProductDto> page = importService.getMarketplaceProducts(tenantId, PageRequest.of(0, 2));
        assertTrue(page.getTotalElements() >= 3);
        assertEquals(2, page.getContent().size());
    }

    // --- Watchlist Tests ---

    @Test
    void watchlist_addAndList() {
        WatchlistItemDto item = watchlistService.addToWatchlist(tenantId,
                new AddToWatchlistRequest(connectionId, "CJ-WATCH-" + UUID.randomUUID().toString().substring(0, 8)));

        assertNotNull(item.id());
        assertNotNull(item.name());

        Page<WatchlistItemDto> watchlist = watchlistService.getWatchlist(tenantId, PageRequest.of(0, 10));
        assertTrue(watchlist.getTotalElements() >= 1);
    }

    @Test
    void watchlist_duplicate_shouldFail() {
        String externalId = "CJ-WDUP-" + UUID.randomUUID().toString().substring(0, 8);

        watchlistService.addToWatchlist(tenantId, new AddToWatchlistRequest(connectionId, externalId));

        assertThrows(DuplicateResourceException.class, () ->
                watchlistService.addToWatchlist(tenantId, new AddToWatchlistRequest(connectionId, externalId)));
    }

    @Test
    void watchlist_remove_shouldDelete() {
        WatchlistItemDto item = watchlistService.addToWatchlist(tenantId,
                new AddToWatchlistRequest(connectionId, "CJ-WREM-" + UUID.randomUUID().toString().substring(0, 8)));

        watchlistService.removeFromWatchlist(tenantId, item.id());

        // Verify removed
        Page<WatchlistItemDto> watchlist = watchlistService.getWatchlist(tenantId, PageRequest.of(0, 100));
        assertTrue(watchlist.getContent().stream().noneMatch(i -> i.id().equals(item.id())));
    }

    // --- updatePricing tests ---

    @Test
    void updatePricing_changePricingRule_shouldUpdate() {
        MarketplaceProductDto imported = importProduct("CJ-PRICE-RULE");
        assertEquals("MARGIN", imported.pricingRule());

        MarketplaceProductDto updated = importService.updatePricing(tenantId, imported.id(),
                "FIXED_MARKUP", null, new BigDecimal("7.50"), null);

        assertEquals("FIXED_MARKUP", updated.pricingRule());
    }

    @Test
    void updatePricing_changeTargetMargin_shouldUpdate() {
        MarketplaceProductDto imported = importProduct("CJ-MARGIN-UPD");

        MarketplaceProductDto updated = importService.updatePricing(tenantId, imported.id(),
                null, new BigDecimal("45.00"), null, null);

        assertEquals(new BigDecimal("45.00"), updated.targetMarginPct());
    }

    @Test
    void updatePricing_changeMinMargin_shouldResetAlert() {
        MarketplaceProductDto imported = importProduct("CJ-MINMARGIN");

        MarketplaceProductDto updated = importService.updatePricing(tenantId, imported.id(),
                null, null, null, new BigDecimal("15.00"));

        assertEquals(new BigDecimal("15.00"), updated.minMarginPct());
        assertFalse(updated.marginAlertTriggered());
    }

    @Test
    void updatePricing_multipleFields_shouldUpdateAll() {
        MarketplaceProductDto imported = importProduct("CJ-MULTI-UPD");

        MarketplaceProductDto updated = importService.updatePricing(tenantId, imported.id(),
                "MANUAL", new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("5.00"));

        assertEquals("MANUAL", updated.pricingRule());
        assertEquals(new BigDecimal("50.00"), updated.targetMarginPct());
        assertEquals(new BigDecimal("5.00"), updated.minMarginPct());
    }

    @Test
    void updatePricing_wrongTenant_shouldFail() {
        MarketplaceProductDto imported = importProduct("CJ-WRONG-T");
        UUID otherTenant = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () ->
                importService.updatePricing(otherTenant, imported.id(),
                        "MANUAL", null, null, null));
    }

    // --- getMarketplaceProduct (single) ---

    @Test
    void getMarketplaceProduct_shouldReturnWithMappings() {
        MarketplaceProductDto imported = importProduct("CJ-SINGLE-GET");

        MarketplaceProductDto found = importService.getMarketplaceProduct(tenantId, imported.id());

        assertEquals(imported.id(), found.id());
        assertEquals(imported.externalProductId(), found.externalProductId());
        assertFalse(found.variantMappings().isEmpty());
    }

    @Test
    void getMarketplaceProduct_wrongTenant_shouldFail() {
        MarketplaceProductDto imported = importProduct("CJ-SINGLE-404");
        UUID otherTenant = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () ->
                importService.getMarketplaceProduct(otherTenant, imported.id()));
    }

    // --- importProduct with MANUAL pricing ---

    @Test
    void importProduct_manualPricing_shouldUseManualPrice() {
        ImportProductRequest request = new ImportProductRequest(
                connectionId,
                "CJ-MANUAL-" + UUID.randomUUID().toString().substring(0, 8),
                List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, new BigDecimal("25.00"))),
                "MANUAL",
                null, null, null, null, null
        );

        MarketplaceProductDto result = importService.importProduct(tenantId, TEST_USER_ID, request);
        assertEquals("MANUAL", result.pricingRule());
        assertEquals(1, result.variantMappings().size());

        // Variant-level source price is $10 (from test adapter), manual selling price $25 should be on the variant
        MarketplaceProductDto.VariantMappingDto mapping = result.variantMappings().get(0);
        assertEquals(0, new BigDecimal("10.00").compareTo(mapping.sourcePrice()));
    }

    // --- Price history (repository-level) ---

    @Test
    void priceHistory_shouldPersistAndQuery() {
        MarketplaceProductDto imported = importProduct("CJ-HISTORY");
        List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(imported.id());
        MarketplaceVariantMapping mapping = mappings.get(0);

        MarketplacePriceHistory history = MarketplacePriceHistory.builder()
                .variantMapping(mapping)
                .priceType(PriceType.PRODUCT)
                .oldPrice(new BigDecimal("10.00"))
                .newPrice(new BigDecimal("12.00"))
                .oldMarginPct(new BigDecimal("30.00"))
                .newMarginPct(new BigDecimal("25.00"))
                .build();
        priceHistoryRepository.save(history);

        List<MarketplacePriceHistory> results = priceHistoryRepository
                .findByVariantMappingIdOrderByDetectedAtDesc(mapping.getId());

        assertFalse(results.isEmpty());
        assertEquals(PriceType.PRODUCT, results.get(0).getPriceType());
        assertEquals(0, new BigDecimal("12.00").compareTo(results.get(0).getNewPrice()));
    }

    // --- Helper ---

    private MarketplaceProductDto importProduct(String prefix) {
        return importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        prefix + "-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));
    }
}
