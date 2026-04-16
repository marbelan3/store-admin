package com.g2u.admin.web.dto;

import java.util.UUID;

public record InventoryItemDto(
        UUID id,
        String type,
        String productName,
        String variantName,
        String sku,
        Integer quantity,
        Integer lowStockThreshold,
        boolean trackInventory
) {
}
