package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String slug,
        String description,
        String imageUrl,
        Integer sortOrder,
        UUID parentId,
        @Size(max = 200) String metaTitle,
        @Size(max = 500) String metaDescription
) {
}
