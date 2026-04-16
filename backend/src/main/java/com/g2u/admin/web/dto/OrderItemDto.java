package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String variantInfo,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
