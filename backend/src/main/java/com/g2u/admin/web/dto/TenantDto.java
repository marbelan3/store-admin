package com.g2u.admin.web.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String primaryColor,
        BigDecimal taxRate,
        String businessEmail,
        String businessPhone,
        String businessAddress,
        String website,
        Map<String, Object> settings,
        boolean active
) {
}
