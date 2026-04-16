package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String notes,
        int totalOrders,
        BigDecimal totalSpent,
        List<CustomerAddressDto> addresses,
        Instant createdAt,
        Instant updatedAt
) {
}
