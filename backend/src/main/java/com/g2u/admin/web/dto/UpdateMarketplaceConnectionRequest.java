package com.g2u.admin.web.dto;

public record UpdateMarketplaceConnectionRequest(
        String apiKey,
        Boolean syncEnabled,
        String defaultWarehouseId,
        String defaultShippingMethod
) {}
