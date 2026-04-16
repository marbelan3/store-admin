package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WebhookDeliveryDto(
        UUID id,
        UUID webhookId,
        String event,
        Map<String, Object> payload,
        Integer responseStatus,
        boolean success,
        int attempt,
        Instant createdAt
) {
}
