package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarketplaceAlertDto(
        UUID marketplaceProductId,
        UUID productId,
        String productName,
        String alertType,
        String message,
        BigDecimal currentMarginPct,
        BigDecimal minMarginPct
) {}
