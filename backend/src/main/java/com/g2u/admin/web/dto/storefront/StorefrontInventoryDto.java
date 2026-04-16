package com.g2u.admin.web.dto.storefront;

public record StorefrontInventoryDto(
        String sku,
        boolean available,
        Integer quantity
) {
}
