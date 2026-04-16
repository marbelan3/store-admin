package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MarketplaceSyncService;
import com.g2u.admin.web.dto.MarketplaceAlertDto;
import com.g2u.admin.web.dto.MarketplaceSyncLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class MarketplaceSyncController {

    private final MarketplaceSyncService syncService;

    public MarketplaceSyncController(MarketplaceSyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/sync-logs/{connectionId}")
    public Page<MarketplaceSyncLogDto> getSyncLogs(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID connectionId,
            @PageableDefault(size = 20) Pageable pageable) {
        return syncService.getSyncLogs(principal.tenantId(), connectionId, pageable);
    }

    @PostMapping("/sync/trigger/{connectionId}")
    public ResponseEntity<Void> triggerSync(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID connectionId) {
        syncService.runAllSyncs(principal.tenantId(), connectionId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/alerts")
    public List<MarketplaceAlertDto> getAlerts(@CurrentUser UserPrincipal principal) {
        return syncService.getAlerts(principal.tenantId());
    }
}
