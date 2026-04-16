package com.g2u.admin.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.domain.product.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight product DTO for list endpoints.
 * Avoids loading lazy collections (variants, media, categories, tags)
 * to prevent N+1 query issues.
 */
public record ProductListDto(
        UUID id,
        String name,
        String slug,
        @JsonIgnore ProductStatus productStatus,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        String sku,
        boolean trackInventory,
        Integer quantity,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        String primaryImageUrl,
        @JsonIgnore ProductSource productSource
) {
    public String status() {
        return productStatus != null ? productStatus.name() : null;
    }

    public String source() {
        return productSource != null ? productSource.name() : "OWN";
    }
}
