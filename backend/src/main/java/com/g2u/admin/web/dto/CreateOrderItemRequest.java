package com.g2u.admin.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        @NotBlank String productName,
        String variantInfo,
        String sku,
        @Min(1) int quantity,
        @NotNull BigDecimal unitPrice
) {
}
