package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.ProductOptionService;
import com.g2u.admin.web.dto.CreateProductOptionRequest;
import com.g2u.admin.web.dto.CreateProductVariantRequest;
import com.g2u.admin.web.dto.ProductOptionDto;
import com.g2u.admin.web.dto.ProductVariantDetailDto;
import com.g2u.admin.web.dto.UpdateProductVariantRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}")
public class ProductOptionController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductOptionController.class);

    private final ProductOptionService productOptionService;

    public ProductOptionController(ProductOptionService productOptionService) {
        this.productOptionService = productOptionService;
    }

    // ---- Options ----

    @GetMapping("/options")
    public List<ProductOptionDto> getOptions(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId) {
        LOG.debug("REST request to get options for product : {}", productId);
        return productOptionService.getOptions(productId, principal.tenantId());
    }

    @PostMapping("/options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ProductOptionDto> createOption(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductOptionRequest request) {
        LOG.debug("REST request to create option for product : {}", productId);
        ProductOptionDto option = productOptionService.createOption(productId, principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
    }

    @DeleteMapping("/options/{optionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteOption(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID optionId) {
        LOG.debug("REST request to delete option {} for product : {}", optionId, productId);
        productOptionService.deleteOption(optionId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }

    // ---- Variants ----

    @GetMapping("/variants")
    public List<ProductVariantDetailDto> getVariants(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId) {
        LOG.debug("REST request to get variants for product : {}", productId);
        return productOptionService.getVariants(productId, principal.tenantId());
    }

    @PostMapping("/variants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ProductVariantDetailDto> createVariant(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        LOG.debug("REST request to create variant for product : {}", productId);
        ProductVariantDetailDto variant = productOptionService.createVariant(
                productId, principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ProductVariantDetailDto updateVariant(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        LOG.debug("REST request to update variant {} for product : {}", variantId, productId);
        return productOptionService.updateVariant(variantId, principal.tenantId(), request);
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteVariant(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        LOG.debug("REST request to delete variant {} for product : {}", variantId, productId);
        productOptionService.deleteVariant(variantId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }
}
