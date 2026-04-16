package com.g2u.admin.web.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(max = 300) String name,
        @Size(max = 300) String slug,
        String description,
        @Size(max = 500) String shortDescription,
        String status,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        @Size(max = 100) String sku,
        @Size(max = 100) String barcode,
        Boolean trackInventory,
        Integer quantity,
        BigDecimal weight,
        String weightUnit,
        @Size(max = 200) String metaTitle,
        @Size(max = 500) String metaDescription,
        Map<String, Object> attributes,
        List<CreateProductRequest.VariantRequest> variants,
        List<CreateProductRequest.MediaRequest> media,
        Set<UUID> categoryIds,
        Set<UUID> tagIds
) {
}
