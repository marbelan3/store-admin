package com.g2u.admin.web.dto;

import java.math.BigDecimal;

public record ValidateDiscountResponse(
        boolean valid,
        String message,
        String discountName,
        String type,
        BigDecimal value,
        BigDecimal discountAmount
) {}
