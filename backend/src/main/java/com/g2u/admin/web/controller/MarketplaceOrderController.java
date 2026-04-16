package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MarketplaceOrderService;
import com.g2u.admin.web.dto.MarketplaceOrderResultDto;
import com.g2u.admin.web.dto.PlaceMarketplaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace/orders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class MarketplaceOrderController {

    private final MarketplaceOrderService orderService;

    public MarketplaceOrderController(MarketplaceOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}/place")
    public ResponseEntity<MarketplaceOrderResultDto> placeOrder(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody PlaceMarketplaceOrderRequest request) {
        MarketplaceOrderResultDto result = orderService.placeOrder(
                principal.tenantId(), orderId, request);
        return ResponseEntity.ok(result);
    }
}
