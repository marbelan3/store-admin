package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record MarketplaceConnectionDto(
        UUID id,
        String provider,
        String email,
        String status,
        boolean syncEnabled,
        String defaultWarehouseId,
        String defaultShippingMethod,
        Instant lastConnectedAt,
        Instant createdAt,
        Instant updatedAt
) {}
