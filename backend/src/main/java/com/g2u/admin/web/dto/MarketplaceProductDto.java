package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MarketplaceProductDto(
        UUID id,
        UUID productId,
        String productName,
        String externalProductId,
        String syncStatus,
        String pricingRule,
        BigDecimal targetMarginPct,
        BigDecimal currentMarginPct,
        BigDecimal minMarginPct,
        boolean marginAlertTriggered,
        boolean excluded,
        Integer lowStockThreshold,
        boolean stockAlertSent,
        List<VariantMappingDto> variantMappings,
        Instant createdAt,
        Instant updatedAt
) {
    public record VariantMappingDto(
            UUID id,
            UUID variantId,
            String variantName,
            String cjVariantId,
            String cjSku,
            String previousCjSku,
            String warehouseId,
            String warehouseCountry,
            BigDecimal sourcePrice,
            BigDecimal shippingEstimate,
            Integer stockQuantity,
            Instant stockLastCheckedAt,
            String skuStatus
    ) {}
}
