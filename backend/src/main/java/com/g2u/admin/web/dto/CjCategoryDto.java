package com.g2u.admin.web.dto;

import java.util.List;

public record CjCategoryDto(
        String name,
        List<SubCategory> subCategories
) {
    public record SubCategory(
            String name,
            List<LeafCategory> categories
    ) {}

    public record LeafCategory(
            String categoryId,
            String categoryName
    ) {}
}
