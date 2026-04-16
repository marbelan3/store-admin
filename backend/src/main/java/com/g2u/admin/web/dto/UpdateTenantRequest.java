package com.g2u.admin.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateTenantRequest(
        @Size(max = 200) String name,
        @Size(max = 500) String logoUrl,
        @Size(max = 20)
        @Pattern(regexp = "^#[0-9A-Fa-f]{3,8}$", message = "must be a valid hex color (e.g. #4F46E5)")
        String primaryColor,
        @DecimalMin("0.00") @DecimalMax("100.00")
        BigDecimal taxRate,
        @Email @Size(max = 255) String businessEmail,
        @Size(max = 50) String businessPhone,
        @Size(max = 1000) String businessAddress,
        @Size(max = 500) String website,
        Map<String, Object> settings
) {
}
