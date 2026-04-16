package com.g2u.admin.web.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
        long totalProducts,
        long activeProducts,
        long draftProducts,
        long archivedProducts,
        long totalCategories,
        long totalTags,
        long totalUsers,
        Map<String, Long> productsByStatus,
        long lowStockCount,
        long noImagesCount,
        long uncategorizedCount,
        List<RecentProductDto> recentlyUpdated,
        long totalOrders,
        long pendingOrders,
        long totalCustomers
) {
}
