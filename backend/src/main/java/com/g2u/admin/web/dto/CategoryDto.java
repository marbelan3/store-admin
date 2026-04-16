package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String path,
        Integer sortOrder,
        boolean active,
        UUID parentId,
        String metaTitle,
        String metaDescription,
        Instant createdAt,
        List<CategoryDto> children
) {
}
