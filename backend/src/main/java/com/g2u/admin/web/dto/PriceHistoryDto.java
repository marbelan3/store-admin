package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceHistoryDto(
        UUID id,
        String priceType,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal oldMarginPct,
        BigDecimal newMarginPct,
        Instant detectedAt
) {}
