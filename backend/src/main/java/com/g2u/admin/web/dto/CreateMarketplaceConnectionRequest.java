package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMarketplaceConnectionRequest(
        @NotNull String provider,
        @NotBlank String apiKey,
        String defaultWarehouseId,
        String defaultShippingMethod
) {}
