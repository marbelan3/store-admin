package com.g2u.admin.web.controller;

import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.ProductService;
import com.g2u.admin.web.dto.BulkProductActionRequest;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.ProductDto;
import com.g2u.admin.web.dto.ProductListDto;
import com.g2u.admin.web.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductListDto> listProducts(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String source,
            @PageableDefault(size = 20) Pageable pageable) {
        ProductStatus productStatus = status != null ? ProductStatus.valueOf(status.toUpperCase()) : null;
        ProductSource productSource = source != null ? ProductSource.valueOf(source.toUpperCase()) : null;
        return productService.getProducts(
                principal.tenantId(), search, productStatus, categoryId, priceMin, priceMax, productSource, pageable);
    }

    @GetMapping("/{id}")
    public ProductDto getProduct(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return productService.getProduct(principal.tenantId(), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ProductDto> createProduct(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto product = productService.createProduct(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ProductDto updateProduct(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(principal.tenantId(), principal.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        productService.deleteProduct(principal.tenantId(), principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkAction(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody BulkProductActionRequest request) {
        int affected = productService.bulkAction(principal.tenantId(), request);
        return ResponseEntity.ok(Map.of("affected", affected));
    }
}
