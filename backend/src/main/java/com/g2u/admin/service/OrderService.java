package com.g2u.admin.service;

import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.order.Order;
import com.g2u.admin.domain.order.OrderItem;
import com.g2u.admin.domain.order.OrderRepository;
import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.domain.order.OrderStatusHistory;
import com.g2u.admin.domain.order.OrderStatusHistoryRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.web.dto.CreateOrderItemRequest;
import com.g2u.admin.web.dto.CreateOrderRequest;
import com.g2u.admin.web.dto.OrderDto;
import com.g2u.admin.web.dto.OrderItemDto;
import com.g2u.admin.web.dto.OrderStatsDto;
import com.g2u.admin.web.dto.OrderStatusHistoryDto;
import com.g2u.admin.web.dto.UpdateOrderStatusRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        OrderStatusHistoryRepository statusHistoryRepository,
                        ProductRepository productRepository,
                        AuditService auditService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public OrderDto createOrder(UUID tenantId, UUID userId, CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .status(OrderStatus.PENDING)
                .subtotal(request.subtotal())
                .taxAmount(request.taxAmount())
                .shippingAmount(request.shippingAmount())
                .totalAmount(request.totalAmount())
                .currency(request.currency() != null ? request.currency() : "USD")
                .shippingAddress(request.shippingAddress())
                .billingAddress(request.billingAddress())
                .notes(request.notes())
                .items(new ArrayList<>())
                .build();
        order.setTenantId(tenantId);

        for (CreateOrderItemRequest itemReq : request.items()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemReq.productId())
                    .variantId(itemReq.variantId())
                    .productName(itemReq.productName())
                    .variantInfo(itemReq.variantInfo())
                    .sku(itemReq.sku())
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .totalPrice(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                    .build();
            order.getItems().add(item);
        }

        order = orderRepository.save(order);
        log.info("Created order (id={}) for tenant {}", order.getId(), tenantId);

        // Record status history: null -> PENDING
        recordStatusHistory(order, null, OrderStatus.PENDING, userId, "Order created");

        auditService.log(tenantId, userId, "CREATE", "ORDER", order.getId(),
                Map.of("customerName", order.getCustomerName(), "totalAmount", order.getTotalAmount().toString()));

        notificationService.createNotification(tenantId, null, NotificationType.ORDER_CREATED,
                "New order created",
                "Order for " + order.getCustomerName() + " — " + order.getCurrency() + " " + order.getTotalAmount());

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID tenantId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrders(UUID tenantId, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (status != null) {
            page = orderRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = orderRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toDto);
    }

    public OrderDto updateStatus(UUID orderId, UUID tenantId, UUID userId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus fromStatus = order.getStatus();
        OrderStatus toStatus = request.status();

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + fromStatus + " to " + toStatus);
        }

        order.setStatus(toStatus);
        order = orderRepository.save(order);

        recordStatusHistory(order, fromStatus, toStatus, userId, request.note());

        // Inventory adjustments
        if (toStatus == OrderStatus.CONFIRMED) {
            deductInventory(tenantId, order);
        } else if (toStatus == OrderStatus.CANCELLED
                && (fromStatus == OrderStatus.CONFIRMED || fromStatus == OrderStatus.PROCESSING)) {
            restoreInventory(tenantId, order);
        }

        log.info("Order {} status changed {} -> {} for tenant {}", orderId, fromStatus, toStatus, tenantId);

        auditService.log(tenantId, userId, "STATUS_CHANGE", "ORDER", order.getId(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()));

        notificationService.createNotification(tenantId, null, NotificationType.ORDER_STATUS_CHANGED,
                "Order status updated",
                "Order " + orderId + " changed from " + fromStatus + " to " + toStatus);

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDto> getStatusHistory(UUID orderId, UUID tenantId) {
        // Verify the order belongs to the tenant
        orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        return statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderStatsDto getOrderStats(UUID tenantId) {
        long total = orderRepository.countByTenantId(tenantId);
        long pending = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.PENDING);
        long confirmed = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.CONFIRMED);
        long processing = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.PROCESSING);
        long shipped = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.SHIPPED);
        long delivered = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.CANCELLED);
        long refunded = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.REFUNDED);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("PENDING", pending);
        byStatus.put("CONFIRMED", confirmed);
        byStatus.put("PROCESSING", processing);
        byStatus.put("SHIPPED", shipped);
        byStatus.put("DELIVERED", delivered);
        byStatus.put("CANCELLED", cancelled);
        byStatus.put("REFUNDED", refunded);

        return new OrderStatsDto(total, pending, confirmed, processing, shipped, delivered, cancelled, refunded, byStatus);
    }

    private void deductInventory(UUID tenantId, Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() != null) {
                Optional<Product> productOpt = productRepository.findByTenantIdAndId(tenantId, item.getProductId());
                productOpt.ifPresent(product -> {
                    int newQty = product.getQuantity() - item.getQuantity();
                    product.setQuantity(Math.max(newQty, 0));
                    productRepository.save(product);
                    log.debug("Deducted {} units from product {} (new qty: {})",
                            item.getQuantity(), product.getId(), product.getQuantity());
                });
            }
        }
    }

    private void restoreInventory(UUID tenantId, Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() != null) {
                Optional<Product> productOpt = productRepository.findByTenantIdAndId(tenantId, item.getProductId());
                productOpt.ifPresent(product -> {
                    product.setQuantity(product.getQuantity() + item.getQuantity());
                    productRepository.save(product);
                    log.debug("Restored {} units to product {} (new qty: {})",
                            item.getQuantity(), product.getId(), product.getQuantity());
                });
            }
        }
    }

    private void recordStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus,
                                     UUID userId, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(userId)
                .note(note)
                .build();
        statusHistoryRepository.save(history);
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getNotes(),
                itemDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductName(),
                item.getVariantInfo(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

    private OrderStatusHistoryDto toHistoryDto(OrderStatusHistory h) {
        return new OrderStatusHistoryDto(
                h.getId(),
                h.getFromStatus() != null ? h.getFromStatus().name() : null,
                h.getToStatus().name(),
                h.getChangedBy(),
                h.getNote(),
                h.getCreatedAt()
        );
    }
}
