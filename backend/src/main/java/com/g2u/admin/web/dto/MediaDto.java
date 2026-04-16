package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record MediaDto(
        UUID id,
        String filename,
        String originalName,
        String mimeType,
        long size,
        String url,
        String altText,
        Instant createdAt
) {}
