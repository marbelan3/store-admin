package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryDto(
        UUID id,
        String fromStatus,
        String toStatus,
        UUID changedBy,
        String note,
        Instant createdAt
) {
}
