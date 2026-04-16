package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CjProductDetailDto(
        String pid,
        String name,
        String description,
        String image,
        String categoryName,
        BigDecimal price,
        List<CjVariantDto> variants
) {
    public record CjVariantDto(
            String vid,
            String name,
            String sku,
            BigDecimal price,
            String image,
            int stock
    ) {}
}
