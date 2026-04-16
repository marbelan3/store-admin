package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DiscountDto(
        UUID id,
        String name,
        String code,
        String type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        int usageCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        List<UUID> productIds,
        List<UUID> categoryIds,
        Instant createdAt,
        Instant updatedAt
) {}
