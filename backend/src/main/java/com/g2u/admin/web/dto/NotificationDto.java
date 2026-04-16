package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID tenantId,
        UUID userId,
        String type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
}
