package com.g2u.admin.web.dto.storefront;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record StorefrontProductDetailDto(
        UUID id,
        String name,
        String slug,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        String sku,
        Map<String, Object> attributes,
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
            Integer quantity,
            Map<String, Object> options,
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
