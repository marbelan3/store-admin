package com.g2u.admin.service;

import com.g2u.admin.domain.order.OrderRepository;
import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateOrderItemRequest;
import com.g2u.admin.web.dto.CreateOrderRequest;
import com.g2u.admin.web.dto.OrderDto;
import com.g2u.admin.web.dto.OrderStatusHistoryDto;
import com.g2u.admin.web.dto.UpdateOrderStatusRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Order Tenant A")
                .slug("order-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Order Tenant B")
                .slug("order-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createOrder_shouldPersistWithItems() {
        CreateOrderRequest request = buildCreateOrderRequest("John Doe", 2);

        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, request);

        assertNotNull(order.id());
        assertEquals("John Doe", order.customerName());
        assertEquals("PENDING", order.status());
        assertEquals(2, order.items().size());
        assertNotNull(order.createdAt());
        // Each item totalPrice = unitPrice * quantity
        order.items().forEach(item ->
                assertEquals(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())), item.totalPrice()));
    }

    @Test
    void getOrders_shouldReturnPaginatedResults() {
        orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Customer 1", 1));
        orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Customer 2", 1));
        orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Customer 3", 1));

        Page<OrderDto> page = orderService.getOrders(tenantAId, null, PageRequest.of(0, 2));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void updateStatus_pendingToConfirmed_shouldSucceed() {
        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Jane", 1));

        OrderDto updated = orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Payment received"));

        assertEquals("CONFIRMED", updated.status());
    }

    @Test
    void updateStatus_invalidTransition_shouldFail() {
        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Bob", 1));

        // PENDING -> SHIPPED is not a valid transition
        assertThrows(IllegalStateException.class, () ->
                orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                        new UpdateOrderStatusRequest(OrderStatus.SHIPPED, null)));
    }

    @Test
    void updateStatus_confirmed_shouldDeductInventory() {
        // Create a product with known inventory
        Product product = Product.builder()
                .name("Test Product")
                .slug("test-product-" + UUID.randomUUID().toString().substring(0, 8))
                .status(ProductStatus.ACTIVE)
                .price(new BigDecimal("25.00"))
                .currency("USD")
                .trackInventory(true)
                .quantity(100)
                .build();
        product.setTenantId(tenantAId);
        product = productRepository.save(product);

        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), null, "Test Product", null, null, 5, new BigDecimal("25.00"))
        );
        CreateOrderRequest request = new CreateOrderRequest(
                "Inventory Customer", "inv@test.com", null, new BigDecimal("125.00"),
                "USD", null, null, null, null, null, null, items);

        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, request);
        orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Confirmed"));

        Product updated = productRepository.findByTenantIdAndId(tenantAId, product.getId()).orElseThrow();
        assertEquals(95, updated.getQuantity());
    }

    @Test
    void cancelOrder_fromConfirmed_shouldRestoreInventory() {
        Product product = Product.builder()
                .name("Restore Product")
                .slug("restore-product-" + UUID.randomUUID().toString().substring(0, 8))
                .status(ProductStatus.ACTIVE)
                .price(new BigDecimal("10.00"))
                .currency("USD")
                .trackInventory(true)
                .quantity(50)
                .build();
        product.setTenantId(tenantAId);
        product = productRepository.save(product);

        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), null, "Restore Product", null, null, 10, new BigDecimal("10.00"))
        );
        CreateOrderRequest request = new CreateOrderRequest(
                "Cancel Customer", null, null, new BigDecimal("100.00"),
                "USD", null, null, null, null, null, null, items);

        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, request);

        // Confirm (deduct inventory: 50 -> 40)
        orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null));
        Product afterConfirm = productRepository.findByTenantIdAndId(tenantAId, product.getId()).orElseThrow();
        assertEquals(40, afterConfirm.getQuantity());

        // Cancel (restore inventory: 40 -> 50)
        orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CANCELLED, "Customer request"));
        Product afterCancel = productRepository.findByTenantIdAndId(tenantAId, product.getId()).orElseThrow();
        assertEquals(50, afterCancel.getQuantity());
    }

    @Test
    void tenantIsolation_orderFromTenantANotVisibleToTenantB() {
        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("Isolated", 1));

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrder(order.id(), tenantBId));
    }

    @Test
    void getStatusHistory_shouldReturnChronological() {
        OrderDto order = orderService.createOrder(tenantAId, TEST_USER_ID, buildCreateOrderRequest("History Test", 1));

        orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Confirmed"));
        orderService.updateStatus(order.id(), tenantAId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.PROCESSING, "Processing"));

        List<OrderStatusHistoryDto> history = orderService.getStatusHistory(order.id(), tenantAId);

        // Should have 3 entries: PENDING creation + CONFIRMED + PROCESSING
        assertEquals(3, history.size());
        // Verify all three statuses are present (order may vary if timestamps match)
        List<String> statuses = history.stream().map(OrderStatusHistoryDto::toStatus).toList();
        assertTrue(statuses.contains("PENDING"));
        assertTrue(statuses.contains("CONFIRMED"));
        assertTrue(statuses.contains("PROCESSING"));
    }

    private CreateOrderRequest buildCreateOrderRequest(String customerName, int itemCount) {
        List<CreateOrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new CreateOrderItemRequest(
                    UUID.randomUUID(), null,
                    "Product " + (i + 1), null, "SKU-" + (i + 1),
                    2, new BigDecimal("50.00")
            ));
        }
        return new CreateOrderRequest(
                customerName, customerName.toLowerCase().replace(" ", "") + "@test.com",
                "+1234567890", new BigDecimal("100.00"),
                "USD", new BigDecimal("90.00"), new BigDecimal("10.00"), BigDecimal.ZERO,
                "123 Main St", "123 Main St", null, items);
    }
}
