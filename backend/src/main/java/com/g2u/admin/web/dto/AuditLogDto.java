package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID tenantId,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> changes,
        Instant createdAt
) {
}
