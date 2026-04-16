package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductMediaDetailDto(
        UUID id,
        UUID mediaId,
        String url,
        String altText,
        String mediaType,
        int sortOrder,
        boolean primary,
        Instant createdAt
) {}
