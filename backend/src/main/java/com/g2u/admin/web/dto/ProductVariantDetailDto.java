package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductVariantDetailDto(
        UUID id,
        String name,
        String sku,
        BigDecimal price,
        BigDecimal compareAtPrice,
        BigDecimal costPrice,
        Integer quantity,
        Integer lowStockThreshold,
        Integer sortOrder,
        boolean active,
        List<ProductOptionDto.OptionValueDto> optionValues,
        Instant createdAt,
        Instant updatedAt
) {}
