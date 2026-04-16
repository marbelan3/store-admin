package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WatchlistItemDto(
        UUID id,
        String externalProductId,
        String name,
        String imageUrl,
        BigDecimal price,
        String stockStatus,
        Instant lastCheckedAt,
        Instant addedAt
) {}
