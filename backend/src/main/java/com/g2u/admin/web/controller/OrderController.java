package com.g2u.admin.web.controller;

import com.g2u.admin.domain.order.OrderStatus;
import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.OrderService;
import com.g2u.admin.web.dto.CreateOrderRequest;
import com.g2u.admin.web.dto.OrderDto;
import com.g2u.admin.web.dto.OrderStatsDto;
import com.g2u.admin.web.dto.OrderStatusHistoryDto;
import com.g2u.admin.web.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderDto> listOrders(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;
        return orderService.getOrders(principal.tenantId(), orderStatus, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<OrderDto> createOrder(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public OrderDto getOrder(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return orderService.getOrder(id, principal.tenantId());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public OrderDto updateStatus(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, principal.tenantId(), principal.userId(), request);
    }

    @GetMapping("/{id}/history")
    public List<OrderStatusHistoryDto> getStatusHistory(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        return orderService.getStatusHistory(id, principal.tenantId());
    }

    @GetMapping("/stats")
    public OrderStatsDto getStats(@CurrentUser UserPrincipal principal) {
        return orderService.getOrderStats(principal.tenantId());
    }
}
