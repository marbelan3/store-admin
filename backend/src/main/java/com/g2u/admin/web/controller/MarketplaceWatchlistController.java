package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MarketplaceWatchlistService;
import com.g2u.admin.web.dto.AddToWatchlistRequest;
import com.g2u.admin.web.dto.WatchlistItemDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace/watchlist")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class MarketplaceWatchlistController {

    private final MarketplaceWatchlistService watchlistService;

    public MarketplaceWatchlistController(MarketplaceWatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public Page<WatchlistItemDto> listWatchlist(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return watchlistService.getWatchlist(principal.tenantId(), pageable);
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDto> addToWatchlist(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody AddToWatchlistRequest request) {
        WatchlistItemDto dto = watchlistService.addToWatchlist(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromWatchlist(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        watchlistService.removeFromWatchlist(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
