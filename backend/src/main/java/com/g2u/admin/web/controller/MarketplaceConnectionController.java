package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MarketplaceConnectionService;
import com.g2u.admin.web.dto.CreateMarketplaceConnectionRequest;
import com.g2u.admin.web.dto.MarketplaceConnectionDto;
import com.g2u.admin.web.dto.UpdateMarketplaceConnectionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace/connections")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class MarketplaceConnectionController {

    private final MarketplaceConnectionService connectionService;

    public MarketplaceConnectionController(MarketplaceConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping
    public List<MarketplaceConnectionDto> listConnections(@CurrentUser UserPrincipal principal) {
        return connectionService.getConnections(principal.tenantId());
    }

    @GetMapping("/{id}")
    public MarketplaceConnectionDto getConnection(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return connectionService.getConnection(principal.tenantId(), id);
    }

    @PostMapping
    public ResponseEntity<MarketplaceConnectionDto> createConnection(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateMarketplaceConnectionRequest request) {
        MarketplaceConnectionDto dto = connectionService.createConnection(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public MarketplaceConnectionDto updateConnection(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMarketplaceConnectionRequest request) {
        return connectionService.updateConnection(principal.tenantId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        connectionService.deleteConnection(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public MarketplaceConnectionDto testConnection(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return connectionService.testConnection(principal.tenantId(), id);
    }
}
