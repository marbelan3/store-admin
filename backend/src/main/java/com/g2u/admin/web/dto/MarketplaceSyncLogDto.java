package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record MarketplaceSyncLogDto(
        UUID id,
        UUID connectionId,
        String syncType,
        String status,
        Integer itemsChecked,
        Integer itemsUpdated,
        Integer errorsCount,
        String errorDetails,
        Instant startedAt,
        Instant completedAt
) {}
