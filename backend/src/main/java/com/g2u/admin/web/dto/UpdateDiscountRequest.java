package com.g2u.admin.web.dto;

import com.g2u.admin.domain.discount.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UpdateDiscountRequest(
        String name,
        String code,
        DiscountType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        List<UUID> productIds,
        List<UUID> categoryIds
) {}
