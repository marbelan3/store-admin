package com.g2u.admin.web.dto;

import java.util.Map;

public record OrderStatsDto(
        long totalOrders,
        long pendingOrders,
        long confirmedOrders,
        long processingOrders,
        long shippedOrders,
        long deliveredOrders,
        long cancelledOrders,
        long refundedOrders,
        Map<String, Long> ordersByStatus
) {
}
