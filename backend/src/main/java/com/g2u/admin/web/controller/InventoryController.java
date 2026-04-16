package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.InventoryService;
import com.g2u.admin.web.dto.BulkInventoryUpdateRequest;
import com.g2u.admin.web.dto.InventoryItemDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class InventoryController {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Page<InventoryItemDto> getInventory(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean outOfStock,
            @PageableDefault(size = 20) Pageable pageable) {
        LOG.debug("REST request to get inventory for tenant: {}", principal.tenantId());
        return inventoryService.getInventory(principal.tenantId(), lowStock, outOfStock, pageable);
    }

    @PatchMapping("/bulk")
    public ResponseEntity<List<InventoryItemDto>> bulkUpdate(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody BulkInventoryUpdateRequest request) {
        LOG.debug("REST request to bulk update inventory, items: {}", request.items().size());
        List<InventoryItemDto> updated = inventoryService.bulkUpdateQuantities(principal.tenantId(), request);
        return ResponseEntity.ok(updated);
    }
}
