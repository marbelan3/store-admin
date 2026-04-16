package com.g2u.admin.web.dto.storefront;

import java.util.List;
import java.util.UUID;

public record StorefrontCategoryDto(
        UUID id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String metaTitle,
        String metaDescription,
        List<StorefrontCategoryDto> children
) {
}
