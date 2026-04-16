package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 300) String name,
        @Size(max = 300) String slug,
        String description,
        @Size(max = 500) String shortDescription,
        String status,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        @Size(max = 100) String sku,
        @Size(max = 100) String barcode,
        boolean trackInventory,
        Integer quantity,
        BigDecimal weight,
        String weightUnit,
        @Size(max = 200) String metaTitle,
        @Size(max = 500) String metaDescription,
        Map<String, Object> attributes,
        List<VariantRequest> variants,
        List<MediaRequest> media,
        Set<UUID> categoryIds,
        Set<UUID> tagIds
) {
    public record VariantRequest(
            @NotBlank String name,
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

    public record MediaRequest(
            @NotBlank String url,
            String altText,
            String mediaType,
            Integer sortOrder,
            boolean primary
    ) {}
}
