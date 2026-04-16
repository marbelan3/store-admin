package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductVariantRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String sku,
        @NotNull BigDecimal price,
        BigDecimal compareAtPrice,
        BigDecimal costPrice,
        Integer quantity,
        Integer lowStockThreshold,
        Integer sortOrder,
        Boolean active,
        List<UUID> optionValueIds
) {}
