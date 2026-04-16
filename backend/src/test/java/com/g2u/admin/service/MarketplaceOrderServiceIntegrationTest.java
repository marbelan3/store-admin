package com.g2u.admin.service;

import com.g2u.admin.config.TestMarketplaceConfig;
import com.g2u.admin.domain.order.Order;
import com.g2u.admin.domain.order.OrderRepository;
import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.*;
import com.g2u.admin.web.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMarketplaceConfig.class)
class MarketplaceOrderServiceIntegrationTest {

    @Autowired
    private MarketplaceOrderService marketplaceOrderService;

    @Autowired
    private MarketplaceImportService importService;

    @Autowired
    private MarketplaceConnectionService connectionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;
    private UUID connectionId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TestMarketplaceConfig.resetFlags();

        Tenant tenant = Tenant.builder()
                .name("Order Tenant")
                .slug("mkt-order-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        MarketplaceConnectionDto conn = connectionService.createConnection(tenantId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "order@test.com", "order-key", "CN", "standard"));
        connectionId = conn.id();
    }

    @Test
    void placeOrder_shouldCreateCjOrder() {
        // Import a marketplace product
        MarketplaceProductDto imported = importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        "CJ-ORD-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", "CN", "CN", null)),
                        null, null, null, null, null, null));

        // Create an order with the marketplace product
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Test Customer", "test@example.com", "+380123456789",
                new BigDecimal("13.00"), "USD", null, null, BigDecimal.ZERO,
                "123 Test St", "123 Test St", null,
                List.of(new CreateOrderItemRequest(
                        imported.productId(),
                        imported.variantMappings().get(0).variantId(),
                        "Test Product",
                        "Variant A",
                        "CJ-SKU-1",
                        1,
                        new BigDecimal("13.00")
                ))
        );

        OrderDto order = orderService.createOrder(tenantId, TEST_USER_ID, orderRequest);

        // Confirm the order (required state for CJ placement)
        orderService.updateStatus(order.id(), tenantId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Confirmed"));

        // Place on CJ
        PlaceMarketplaceOrderRequest placeRequest = new PlaceMarketplaceOrderRequest(
                "UA", "Kyiv", "Kyiv", "123 Main St", "01001", "Test Customer", "+380123456789");

        MarketplaceOrderResultDto result = marketplaceOrderService.placeOrder(tenantId, order.id(), placeRequest);

        assertNotNull(result.cjOrderId());
        assertEquals(order.id(), result.orderId());

        // Verify order has CJ order ID
        Order updated = orderRepository.findByIdAndTenantId(order.id(), tenantId).orElseThrow();
        assertNotNull(updated.getCjOrderId());
        assertEquals(OrderStatus.PROCESSING, updated.getStatus());
    }

    @Test
    void placeOrder_alreadyPlaced_shouldFail() {
        MarketplaceProductDto imported = importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        "CJ-DUP-ORD-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Dup Customer", "dup@test.com", null,
                new BigDecimal("13.00"), "USD", null, null, BigDecimal.ZERO,
                "Addr", "Addr", null,
                List.of(new CreateOrderItemRequest(imported.productId(),
                        imported.variantMappings().get(0).variantId(),
                        "Product", null, "SKU", 1, new BigDecimal("13.00"))));

        OrderDto order = orderService.createOrder(tenantId, TEST_USER_ID, orderRequest);
        orderService.updateStatus(order.id(), tenantId, TEST_USER_ID,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null));

        PlaceMarketplaceOrderRequest placeReq = new PlaceMarketplaceOrderRequest(
                "UA", "Kyiv", "Kyiv", "Addr", "01001", "Name", "+380");

        // First placement
        marketplaceOrderService.placeOrder(tenantId, order.id(), placeReq);

        // Second placement should fail
        assertThrows(BusinessRuleViolationException.class, () ->
                marketplaceOrderService.placeOrder(tenantId, order.id(), placeReq));
    }

    @Test
    void placeOrder_wrongStatus_shouldFail() {
        MarketplaceProductDto imported = importService.importProduct(tenantId, TEST_USER_ID,
                new ImportProductRequest(connectionId,
                        "CJ-STATUS-" + UUID.randomUUID().toString().substring(0, 8),
                        List.of(new ImportProductRequest.ImportVariantRequest("vid-1", "SKU-1", null, null, null)),
                        null, null, null, null, null, null));

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Status Customer", "status@test.com", null,
                new BigDecimal("13.00"), "USD", null, null, BigDecimal.ZERO,
                "Addr", "Addr", null,
                List.of(new CreateOrderItemRequest(imported.productId(),
                        imported.variantMappings().get(0).variantId(),
                        "Product", null, "SKU", 1, new BigDecimal("13.00"))));

        OrderDto order = orderService.createOrder(tenantId, TEST_USER_ID, orderRequest);
        // Order is PENDING — not valid for CJ placement

        assertThrows(BusinessRuleViolationException.class, () ->
                marketplaceOrderService.placeOrder(tenantId, order.id(),
                        new PlaceMarketplaceOrderRequest("UA", "Kyiv", "Kyiv", "Addr", "01001", "Name", "+380")));
    }

    @Test
    void pollOrderStatuses_shouldNotFailOnEmptyOrders() {
        // Should work without error even with no CJ orders
        assertDoesNotThrow(() -> marketplaceOrderService.pollOrderStatuses());
    }

    @Test
    void pollTracking_shouldNotFailOnEmptyOrders() {
        assertDoesNotThrow(() -> marketplaceOrderService.pollTracking());
    }
}
