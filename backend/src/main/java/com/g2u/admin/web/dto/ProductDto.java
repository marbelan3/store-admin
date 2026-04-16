package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String slug,
        String description,
        String shortDescription,
        String status,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        String sku,
        String barcode,
        boolean trackInventory,
        Integer quantity,
        BigDecimal weight,
        String weightUnit,
        String metaTitle,
        String metaDescription,
        Map<String, Object> attributes,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<VariantDto> variants,
        List<MediaDto> media,
        Set<CategoryRefDto> categories,
        Set<TagRefDto> tags
) {
    public record VariantDto(
            UUID id,
            String name,
            String sku,
            BigDecimal price,
            BigDecimal compareAtPrice,
            BigDecimal costPrice,
            Integer quantity,
            Integer lowStockThreshold,
            Map<String, Object> options,
            Integer sortOrder,
            boolean active
    ) {}

    public record MediaDto(
            UUID id,
            String url,
            String altText,
            String mediaType,
            Integer sortOrder,
            boolean primary
    ) {}

    public record CategoryRefDto(UUID id, String name, String slug) {}

    public record TagRefDto(UUID id, String name, String slug) {}
}
