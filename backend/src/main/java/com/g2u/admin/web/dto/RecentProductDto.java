package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentProductDto(
        UUID id,
        String name,
        Instant updatedAt
) {
}
