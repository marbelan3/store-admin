package com.g2u.admin.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerName,
        String customerEmail,
        String customerPhone,
        @NotNull BigDecimal totalAmount,
        String currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        String shippingAddress,
        String billingAddress,
        String notes,
        @NotEmpty @Valid List<CreateOrderItemRequest> items
) {
}
