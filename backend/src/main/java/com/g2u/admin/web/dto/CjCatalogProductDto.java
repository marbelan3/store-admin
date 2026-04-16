package com.g2u.admin.web.dto;

import java.math.BigDecimal;

public record CjCatalogProductDto(
        String pid,
        String name,
        String image,
        String categoryName,
        BigDecimal price
) {}
