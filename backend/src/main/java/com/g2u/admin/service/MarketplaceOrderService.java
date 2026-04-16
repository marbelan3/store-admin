package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.MarketplaceConnection;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import com.g2u.admin.domain.marketplace.MarketplaceProduct;
import com.g2u.admin.domain.marketplace.MarketplaceProductRepository;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMapping;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMappingRepository;
import com.g2u.admin.domain.marketplace.ConnectionStatus;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.order.Order;
import com.g2u.admin.domain.order.OrderItem;
import com.g2u.admin.domain.order.OrderRepository;
import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.infrastructure.marketplace.CjApiException;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjOrderRequest;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjOrderResult;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjOrderStatus;
import com.g2u.admin.web.dto.MarketplaceOrderResultDto;
import com.g2u.admin.web.dto.PlaceMarketplaceOrderRequest;
import com.g2u.admin.web.exception.BusinessRuleViolationException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceOrderService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceOrderService.class);
    private static final BigDecimal PRICE_DIFF_THRESHOLD = new BigDecimal("0.05"); // 5%

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;
    private final NotificationService notificationService;

    public MarketplaceOrderService(OrderRepository orderRepository,
                                    ProductRepository productRepository,
                                    MarketplaceProductRepository marketplaceProductRepository,
                                    MarketplaceVariantMappingRepository variantMappingRepository,
                                    MarketplaceConnectionRepository connectionRepository,
                                    MarketplaceAdapter marketplaceAdapter,
                                    MarketplaceConnectionService connectionService,
                                    NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.marketplaceProductRepository = marketplaceProductRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.connectionRepository = connectionRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
        this.notificationService = notificationService;
    }

    /**
     * Place marketplace items from an order on CJ Dropshipping.
     * Only marketplace items are sent to CJ; OWN items are handled normally.
     */
    public MarketplaceOrderResultDto placeOrder(UUID tenantId, UUID orderId,
                                                 PlaceMarketplaceOrderRequest request) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getCjOrderId() != null) {
            throw new BusinessRuleViolationException("Order already placed on CJ: " + order.getCjOrderId());
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessRuleViolationException("Order must be CONFIRMED or PROCESSING to place on CJ");
        }

        // Find marketplace items in this order
        List<OrderItem> marketplaceItems = new ArrayList<>();
        List<MarketplaceVariantMapping> mappings = new ArrayList<>();
        MarketplaceConnection connection = null;
        List<String> warnings = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;

            Product product = productRepository.findByTenantIdAndId(tenantId, item.getProductId()).orElse(null);
            if (product == null || product.getSource() != ProductSource.MARKETPLACE) continue;

            marketplaceItems.add(item);

            // Find variant mapping
            if (item.getVariantId() != null) {
                MarketplaceVariantMapping mapping = variantMappingRepository
                        .findByTenantId(tenantId).stream()
                        .filter(m -> m.getVariant() != null && m.getVariant().getId().equals(item.getVariantId()))
                        .findFirst().orElse(null);

                if (mapping != null) {
                    mappings.add(mapping);
                    if (connection == null) {
                        connection = mapping.getMarketplaceProduct().getConnection();
                    }
                }
            }
        }

        if (marketplaceItems.isEmpty()) {
            throw new BusinessRuleViolationException("No marketplace items found in this order");
        }

        if (connection == null) {
            throw new BusinessRuleViolationException("No marketplace connection found for order items");
        }

        String token = connectionService.getValidAccessToken(connection);

        // Pre-order validation: price check
        for (MarketplaceVariantMapping mapping : mappings) {
            try {
                var detail = marketplaceAdapter.getProductDetails(token,
                        mapping.getMarketplaceProduct().getExternalProductId());
                BigDecimal currentPrice = detail.sellPrice();
                if (mapping.getSourcePrice() != null && currentPrice != null) {
                    BigDecimal diff = currentPrice.subtract(mapping.getSourcePrice()).abs()
                            .divide(mapping.getSourcePrice(), 4, RoundingMode.HALF_UP);
                    if (diff.compareTo(PRICE_DIFF_THRESHOLD) > 0) {
                        warnings.add("Price changed for " + mapping.getCjSku()
                                + ": was " + mapping.getSourcePrice() + ", now " + currentPrice);
                    }
                }
            } catch (Exception e) {
                warnings.add("Price check failed for " + mapping.getCjSku() + ": " + e.getMessage());
            }
        }

        // Pre-order validation: stock check
        for (MarketplaceVariantMapping mapping : mappings) {
            try {
                int stock = marketplaceAdapter.getStock(token, mapping.getCjVariantId());
                if (stock <= 0) {
                    // Try fallback SKU
                    if (mapping.getFallbackCjSku() != null) {
                        warnings.add("Primary SKU " + mapping.getCjSku()
                                + " out of stock, using fallback: " + mapping.getFallbackCjSku());
                    } else {
                        warnings.add("SKU " + mapping.getCjSku() + " is out of stock on CJ, no fallback available");
                    }
                }
            } catch (Exception e) {
                warnings.add("Stock check failed for " + mapping.getCjSku() + ": " + e.getMessage());
            }
        }

        // Build CJ order
        List<MarketplaceAdapter.CjOrderItem> cjItems = new ArrayList<>();
        for (int i = 0; i < marketplaceItems.size() && i < mappings.size(); i++) {
            OrderItem item = marketplaceItems.get(i);
            MarketplaceVariantMapping mapping = mappings.get(i);
            String sku = mapping.getCjSku();

            cjItems.add(new MarketplaceAdapter.CjOrderItem(
                    sku,
                    item.getQuantity(),
                    connection.getDefaultShippingMethod()
            ));
        }

        CjOrderRequest cjRequest = new CjOrderRequest(
                request.shippingCountry(),
                request.shippingProvince(),
                request.shippingCity(),
                request.shippingAddress(),
                request.shippingZip(),
                request.shippingCustomerName(),
                request.shippingPhone(),
                cjItems
        );

        try {
            CjOrderResult result = marketplaceAdapter.placeOrder(token, cjRequest);
            order.setCjOrderId(result.orderId());
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            log.info("CJ order placed: {} for order {}", result.orderId(), orderId);

            return new MarketplaceOrderResultDto(orderId, result.orderId(), result.status(), warnings);
        } catch (CjApiException e) {
            // Notify seller for manual resolution
            notificationService.createNotification(tenantId, null,
                    NotificationType.SYSTEM,
                    "CJ order placement failed for order " + orderId,
                    e.getMessage());

            throw new BusinessRuleViolationException("CJ order placement failed: " + e.getMessage());
        }
    }

    // --- Polling Jobs ---

    public void pollOrderStatuses() {
        List<Order> pendingOrders = orderRepository.findByCjOrderIdNotNullAndStatusIn(
                Set.of(OrderStatus.PROCESSING));

        for (Order order : pendingOrders) {
            try {
                MarketplaceConnection connection = findConnectionForOrder(order);
                if (connection == null) continue;

                String token = connectionService.getValidAccessToken(connection);
                CjOrderStatus status = marketplaceAdapter.getOrderStatus(token, order.getCjOrderId());

                if ("SHIPPED".equalsIgnoreCase(status.status()) || "IN_TRANSIT".equalsIgnoreCase(status.status())) {
                    order.setStatus(OrderStatus.SHIPPED);
                    if (status.trackingNumber() != null) {
                        order.setCjTrackingNumber(status.trackingNumber());
                    }
                    orderRepository.save(order);
                    log.info("Order {} status updated to SHIPPED, tracking: {}",
                            order.getId(), status.trackingNumber());
                } else if ("DELIVERED".equalsIgnoreCase(status.status())) {
                    order.setStatus(OrderStatus.DELIVERED);
                    orderRepository.save(order);
                } else if ("CANCELLED".equalsIgnoreCase(status.status())) {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    notificationService.createNotification(order.getTenantId(), null,
                            NotificationType.ORDER_STATUS_CHANGED,
                            "CJ cancelled order " + order.getCjOrderId(),
                            "The CJ order was cancelled. Please review.");
                }
            } catch (Exception e) {
                log.warn("Failed to poll status for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    public void pollTracking() {
        List<Order> shippedOrders = orderRepository.findByCjOrderIdNotNullAndStatusIn(
                Set.of(OrderStatus.SHIPPED));

        for (Order order : shippedOrders) {
            if (order.getCjTrackingNumber() != null && !order.getCjTrackingNumber().isBlank()) {
                continue; // Already has tracking
            }

            try {
                MarketplaceConnection connection = findConnectionForOrder(order);
                if (connection == null) continue;

                String token = connectionService.getValidAccessToken(connection);
                var tracking = marketplaceAdapter.getTracking(token, order.getCjOrderId());

                if (tracking.trackingNumber() != null && !tracking.trackingNumber().isBlank()) {
                    order.setCjTrackingNumber(tracking.trackingNumber());
                    orderRepository.save(order);
                    log.info("Tracking number {} set for order {}", tracking.trackingNumber(), order.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to poll tracking for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private MarketplaceConnection findConnectionForOrder(Order order) {
        // Find connection via the marketplace products in this order
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;
            var mpProduct = marketplaceProductRepository.findByTenantId(order.getTenantId(),
                    org.springframework.data.domain.Pageable.unpaged()).stream()
                    .filter(mp -> mp.getProduct().getId().equals(item.getProductId()))
                    .findFirst().orElse(null);
            if (mpProduct != null) {
                return mpProduct.getConnection();
            }
        }
        return null;
    }
}
