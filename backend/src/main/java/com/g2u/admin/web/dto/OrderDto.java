package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        String customerName,
        String customerEmail,
        String customerPhone,
        String status,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        String billingAddress,
        String notes,
        List<OrderItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {
}
