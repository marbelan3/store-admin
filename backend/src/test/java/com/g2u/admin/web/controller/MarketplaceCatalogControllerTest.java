package com.g2u.admin.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2u.admin.config.TestMarketplaceConfig;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistory;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistoryRepository;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMapping;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMappingRepository;
import com.g2u.admin.domain.marketplace.PriceType;
import com.g2u.admin.infrastructure.security.JwtTokenProvider;
import com.g2u.admin.service.MarketplaceConnectionService;
import com.g2u.admin.service.MarketplaceImportService;
import com.g2u.admin.web.dto.CreateMarketplaceConnectionRequest;
import com.g2u.admin.web.dto.ImportProductRequest;
import com.g2u.admin.web.dto.MarketplaceConnectionDto;
import com.g2u.admin.web.dto.MarketplaceProductDto;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestMarketplaceConfig.class)
class MarketplaceCatalogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MarketplaceImportService importService;
    @Autowired private MarketplaceConnectionService connectionService;
    @Autowired private MarketplaceVariantMappingRepository variantMappingRepository;
    @Autowired private MarketplacePriceHistoryRepository priceHistoryRepository;
    @Autowired private TenantRepository tenantRepository;

    private UUID tenantId;
    private UUID userId;
    private String token;
    private UUID connectionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .name("Controller Test Tenant")
                .slug("ctrl-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
        token = jwtTokenProvider.generateAccessToken(userId, tenantId, "admin@test.com", "TENANT_ADMIN");

        MarketplaceConnectionDto conn = connectionService.createConnection(tenantId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "test@test.com", "test-key", "CN", null));
        connectionId = conn.id();
    }

    // --- Pricing endpoint ---

    @Test
    void updatePricing_shouldReturn200() throws Exception {
        MarketplaceProductDto mp = importTestProduct();

        mockMvc.perform(patch("/api/marketplace/products/{id}/pricing", mp.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "pricingRule", "FIXED_MARKUP",
                                "fixedMarkupAmount", 5.00
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricingRule").value("FIXED_MARKUP"));
    }

    @Test
    void updatePricing_asViewer_shouldReturn403() throws Exception {
        MarketplaceProductDto mp = importTestProduct();
        String viewerToken = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), tenantId, "viewer@test.com", "TENANT_VIEWER");

        mockMvc.perform(patch("/api/marketplace/products/{id}/pricing", mp.id())
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pricingRule\":\"MANUAL\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Price history endpoint ---

    @Test
    void getPriceHistory_shouldReturnHistory() throws Exception {
        MarketplaceProductDto mp = importTestProduct();
        List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.id());
        MarketplaceVariantMapping mapping = mappings.get(0);

        // Insert a price history record
        priceHistoryRepository.save(MarketplacePriceHistory.builder()
                .variantMapping(mapping)
                .priceType(PriceType.PRODUCT)
                .oldPrice(new BigDecimal("10.00"))
                .newPrice(new BigDecimal("14.00"))
                .oldMarginPct(new BigDecimal("30.00"))
                .newMarginPct(new BigDecimal("22.00"))
                .build());

        mockMvc.perform(get("/api/marketplace/variants/{mappingId}/price-history", mapping.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priceType").value("PRODUCT"))
                .andExpect(jsonPath("$[0].newPrice").value(14.00));
    }

    @Test
    void getPriceHistory_emptyHistory_shouldReturnEmptyArray() throws Exception {
        MarketplaceProductDto mp = importTestProduct();
        List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.id());

        mockMvc.perform(get("/api/marketplace/variants/{mappingId}/price-history", mappings.get(0).getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Products list/detail ---

    @Test
    void listProducts_shouldReturn200() throws Exception {
        importTestProduct();

        mockMvc.perform(get("/api/marketplace/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getProduct_shouldReturn200() throws Exception {
        MarketplaceProductDto mp = importTestProduct();

        mockMvc.perform(get("/api/marketplace/products/{id}", mp.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mp.id().toString()))
                .andExpect(jsonPath("$.variantMappings").isArray());
    }

    // --- Source filter on /api/products ---

    @Test
    void listProducts_withSourceFilter_shouldFilter() throws Exception {
        importTestProduct(); // creates a MARKETPLACE product

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .param("source", "MARKETPLACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listProducts_withInvalidSource_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .param("source", "INVALID_SOURCE"))
                .andExpect(status().isBadRequest());
    }

    // --- Toggle exclude ---

    @Test
    void toggleExclude_shouldReturn200() throws Exception {
        MarketplaceProductDto mp = importTestProduct();

        mockMvc.perform(patch("/api/marketplace/products/{id}/exclude", mp.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excluded").value(true));
    }

    // --- Update threshold ---

    @Test
    void updateThreshold_shouldReturn200() throws Exception {
        MarketplaceProductDto mp = importTestProduct();

        mockMvc.perform(patch("/api/marketplace/products/{id}/threshold", mp.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockThreshold").value(15));
    }

    // --- Helper ---

    private MarketplaceProductDto importTestProduct() {
        return importService.importProduct(tenantId, userId,
                new ImportProductRequest(connectionId,
                        "CJ-CTRL-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));
    }
}
