package com.g2u.admin.web.dto;

import com.g2u.admin.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        String note
) {
}
