package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record DiscountListDto(
        UUID id,
        String name,
        String code,
        String type,
        BigDecimal value,
        Integer usageLimit,
        int usageCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        Instant createdAt
) {}
