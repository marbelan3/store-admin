package com.g2u.admin.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ImportProductRequest(
        @NotNull UUID connectionId,
        @NotBlank String externalProductId,
        @NotEmpty @Valid List<ImportVariantRequest> variants,
        String pricingRule,
        BigDecimal targetMarginPct,
        BigDecimal fixedMarkupAmount,
        BigDecimal minMarginPct,
        UUID categoryId,
        Integer lowStockThreshold
) {
    public record ImportVariantRequest(
            @NotBlank String cjVariantId,
            @NotBlank String cjSku,
            String warehouseId,
            String warehouseCountry,
            BigDecimal manualPrice
    ) {}
}
