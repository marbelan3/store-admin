package com.g2u.admin.web.dto.storefront;

import java.math.BigDecimal;
import java.util.UUID;

public record StorefrontProductDto(
        UUID id,
        String name,
        String slug,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        String primaryImageUrl
) {
}
