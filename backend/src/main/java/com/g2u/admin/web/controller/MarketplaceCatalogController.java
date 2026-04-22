package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistory;
import com.g2u.admin.domain.marketplace.MarketplacePriceHistoryRepository;
import com.g2u.admin.service.MarketplaceImportService;
import com.g2u.admin.web.dto.CjCatalogProductDto;
import com.g2u.admin.web.dto.CjCategoryDto;
import com.g2u.admin.web.dto.CjProductDetailDto;
import com.g2u.admin.web.dto.ImportProductRequest;
import com.g2u.admin.web.dto.MarketplaceProductDto;
import com.g2u.admin.web.dto.PriceHistoryDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class MarketplaceCatalogController {

    private final MarketplaceImportService importService;
    private final MarketplacePriceHistoryRepository priceHistoryRepository;

    public MarketplaceCatalogController(MarketplaceImportService importService,
                                         MarketplacePriceHistoryRepository priceHistoryRepository) {
        this.importService = importService;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @GetMapping("/cj/catalog")
    public List<CjCatalogProductDto> searchCatalog(
            @CurrentUser UserPrincipal principal,
            @RequestParam UUID connectionId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return importService.searchCatalog(principal.tenantId(), connectionId, q, page, pageSize);
    }

    @GetMapping("/cj/categories")
    public List<CjCategoryDto> getCategories(
            @CurrentUser UserPrincipal principal,
            @RequestParam UUID connectionId) {
        return importService.getCategories(principal.tenantId(), connectionId);
    }

    @GetMapping("/cj/catalog/category")
    public List<CjCatalogProductDto> listByCategory(
            @CurrentUser UserPrincipal principal,
            @RequestParam UUID connectionId,
            @RequestParam String categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return importService.listByCategory(principal.tenantId(), connectionId, categoryId, page, pageSize);
    }

    @GetMapping("/cj/products/{pid}")
    public CjProductDetailDto getProductDetail(
            @CurrentUser UserPrincipal principal,
            @RequestParam UUID connectionId,
            @PathVariable String pid) {
        return importService.getProductDetail(principal.tenantId(), connectionId, pid);
    }

    @PostMapping("/import")
    public ResponseEntity<MarketplaceProductDto> importProduct(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody ImportProductRequest request) {
        MarketplaceProductDto dto = importService.importProduct(
                principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/products")
    public Page<MarketplaceProductDto> listProducts(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return importService.getMarketplaceProducts(principal.tenantId(), pageable);
    }

    @GetMapping("/products/{id}")
    public MarketplaceProductDto getProduct(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        return importService.getMarketplaceProduct(principal.tenantId(), id);
    }

    @PatchMapping("/products/{id}/exclude")
    public MarketplaceProductDto toggleExclude(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        return importService.toggleExcluded(principal.tenantId(), id);
    }

    @PatchMapping("/products/{id}/pricing")
    public MarketplaceProductDto updatePricing(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String pricingRule = body.containsKey("pricingRule") ? (String) body.get("pricingRule") : null;
        java.math.BigDecimal targetMarginPct = body.containsKey("targetMarginPct")
                ? new java.math.BigDecimal(body.get("targetMarginPct").toString()) : null;
        java.math.BigDecimal fixedMarkupAmount = body.containsKey("fixedMarkupAmount")
                ? new java.math.BigDecimal(body.get("fixedMarkupAmount").toString()) : null;
        java.math.BigDecimal minMarginPct = body.containsKey("minMarginPct")
                ? new java.math.BigDecimal(body.get("minMarginPct").toString()) : null;
        return importService.updatePricing(principal.tenantId(), id,
                pricingRule, targetMarginPct, fixedMarkupAmount, minMarginPct);
    }

    @PatchMapping("/products/{id}/threshold")
    public MarketplaceProductDto updateThreshold(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Map<String, Integer> body) {
        return importService.updateThreshold(principal.tenantId(), id, body.get("threshold"));
    }

    @GetMapping("/variants/{mappingId}/price-history")
    public List<PriceHistoryDto> getPriceHistory(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID mappingId) {
        return priceHistoryRepository.findByVariantMappingIdOrderByDetectedAtDesc(mappingId)
                .stream()
                .map(h -> new PriceHistoryDto(
                        h.getId(),
                        h.getPriceType().name(),
                        h.getOldPrice(),
                        h.getNewPrice(),
                        h.getOldMarginPct(),
                        h.getNewMarginPct(),
                        h.getDetectedAt()))
                .toList();
    }
}
