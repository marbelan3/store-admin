package com.g2u.admin.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkInventoryUpdateRequest(
        @NotEmpty @Valid List<InventoryUpdateItem> items
) {
    public record InventoryUpdateItem(
            @NotNull UUID id,
            @NotNull String type,
            @NotNull Integer quantity
    ) {
    }
}
