package com.g2u.admin.web.dto;

import java.util.List;
import java.util.UUID;

public record MarketplaceOrderResultDto(
        UUID orderId,
        String cjOrderId,
        String status,
        List<String> warnings
) {}
