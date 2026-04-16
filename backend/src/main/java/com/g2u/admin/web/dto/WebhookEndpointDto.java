package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookEndpointDto(
        UUID id,
        UUID tenantId,
        String url,
        List<String> events,
        String secret,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
