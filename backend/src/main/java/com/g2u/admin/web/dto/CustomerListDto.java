package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerListDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        int totalOrders,
        BigDecimal totalSpent,
        Instant createdAt
) {
}
