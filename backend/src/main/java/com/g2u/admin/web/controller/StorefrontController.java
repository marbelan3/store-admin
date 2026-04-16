package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.multitenancy.TenantContext;
import com.g2u.admin.service.StorefrontService;
import com.g2u.admin.web.dto.storefront.StorefrontCategoryDto;
import com.g2u.admin.web.dto.storefront.StorefrontInventoryDto;
import com.g2u.admin.web.dto.storefront.StorefrontProductDetailDto;
import com.g2u.admin.web.dto.storefront.StorefrontProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storefront")
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    @GetMapping("/products")
    public Page<StorefrontProductDto> listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Pageable sorted = applySorting(pageable, sort);
        return storefrontService.getProducts(tenantId, category, tag, search, sorted);
    }

    @GetMapping("/products/{slug}")
    public StorefrontProductDetailDto getProduct(@PathVariable String slug) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return storefrontService.getProductBySlug(tenantId, slug);
    }

    @GetMapping("/categories")
    public List<StorefrontCategoryDto> getCategories() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return storefrontService.getCategoryTree(tenantId);
    }

    @GetMapping("/categories/{slug}")
    public StorefrontCategoryDto getCategory(@PathVariable String slug) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return storefrontService.getCategoryBySlug(tenantId, slug);
    }

    @GetMapping("/categories/{slug}/products")
    public Page<StorefrontProductDto> getCategoryProducts(
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return storefrontService.getProductsByCategory(tenantId, slug, pageable);
    }

    @GetMapping("/inventory/{sku}")
    public StorefrontInventoryDto getInventory(@PathVariable String sku) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return storefrontService.getInventory(tenantId, sku);
    }

    @GetMapping("/inventory/bulk")
    public List<StorefrontInventoryDto> getBulkInventory(@RequestParam String skus) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<String> skuList = Arrays.asList(skus.split(","));
        return storefrontService.getBulkInventory(tenantId, skuList);
    }

    private Pageable applySorting(Pageable pageable, String sort) {
        if (sort == null || sort.isBlank()) {
            return pageable;
        }

        Sort sorting = switch (sort.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> pageable.getSort();
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);
    }
}
