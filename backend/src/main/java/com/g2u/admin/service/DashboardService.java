package com.g2u.admin.service;

import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.domain.customer.CustomerRepository;
import com.g2u.admin.domain.order.OrderRepository;
import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.tag.TagRepository;
import com.g2u.admin.domain.user.UserRepository;
import com.g2u.admin.web.dto.DashboardStatsDto;
import com.g2u.admin.web.dto.RecentProductDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public DashboardService(ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            TagRepository tagRepository,
                            UserRepository userRepository,
                            OrderRepository orderRepository,
                            CustomerRepository customerRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    public DashboardStatsDto getStats(UUID tenantId) {
        long total = productRepository.countByTenantId(tenantId);
        long active = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE);
        long draft = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.DRAFT);
        long archived = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ARCHIVED);
        long categories = categoryRepository.countByTenantId(tenantId);
        long tags = tagRepository.countByTenantId(tenantId);
        long users = userRepository.countByTenantId(tenantId);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("ACTIVE", active);
        byStatus.put("DRAFT", draft);
        byStatus.put("ARCHIVED", archived);

        long lowStockCount = productRepository.countLowStock(tenantId);
        long noImagesCount = productRepository.countWithNoMedia(tenantId);
        long uncategorizedCount = productRepository.countUncategorized(tenantId);
        List<RecentProductDto> recentlyUpdated = productRepository.findRecentlyUpdated(
                tenantId, PageRequest.of(0, 10));

        long totalOrders = orderRepository.countByTenantId(tenantId);
        long pendingOrders = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.PENDING);

        long totalCustomers = customerRepository.countByTenantId(tenantId);

        return new DashboardStatsDto(total, active, draft, archived, categories, tags, users, byStatus,
                lowStockCount, noImagesCount, uncategorizedCount, recentlyUpdated,
                totalOrders, pendingOrders, totalCustomers);
    }
}
