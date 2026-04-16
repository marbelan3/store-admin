package com.g2u.admin.web.dto;

import com.g2u.admin.domain.discount.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateDiscountRequest(
        @NotBlank String name,
        String code,
        @NotNull DiscountType type,
        @NotNull BigDecimal value,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<UUID> productIds,
        List<UUID> categoryIds
) {}
