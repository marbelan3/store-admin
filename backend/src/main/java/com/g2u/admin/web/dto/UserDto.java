package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        String role,
        boolean active,
        UUID tenantId,
        Instant createdAt
) {
}
