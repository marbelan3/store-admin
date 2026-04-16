package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.ExternalStockStatus;
import com.g2u.admin.domain.marketplace.MarketplaceConnection;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import com.g2u.admin.domain.marketplace.MarketplaceWatchlistItem;
import com.g2u.admin.domain.marketplace.MarketplaceWatchlistItemRepository;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjProduct;
import com.g2u.admin.web.dto.AddToWatchlistRequest;
import com.g2u.admin.web.dto.WatchlistItemDto;
import com.g2u.admin.web.exception.DuplicateResourceException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceWatchlistService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceWatchlistService.class);

    private final MarketplaceWatchlistItemRepository watchlistRepository;
    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;

    public MarketplaceWatchlistService(MarketplaceWatchlistItemRepository watchlistRepository,
                                        MarketplaceConnectionRepository connectionRepository,
                                        MarketplaceAdapter marketplaceAdapter,
                                        MarketplaceConnectionService connectionService) {
        this.watchlistRepository = watchlistRepository;
        this.connectionRepository = connectionRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
    }

    @Transactional(readOnly = true)
    public Page<WatchlistItemDto> getWatchlist(UUID tenantId, Pageable pageable) {
        return watchlistRepository.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    public WatchlistItemDto addToWatchlist(UUID tenantId, AddToWatchlistRequest request) {
        if (watchlistRepository.existsByTenantIdAndExternalProductId(tenantId, request.externalProductId())) {
            throw new DuplicateResourceException("Product already in watchlist: " + request.externalProductId());
        }

        MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, request.connectionId())
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceConnection", request.connectionId()));

        String token = connectionService.getValidAccessToken(connection);
        CjProduct cjProduct = marketplaceAdapter.getProductDetails(token, request.externalProductId());

        MarketplaceWatchlistItem item = MarketplaceWatchlistItem.builder()
                .connection(connection)
                .externalProductId(request.externalProductId())
                .externalProductName(cjProduct.productName())
                .externalImageUrl(cjProduct.productImage())
                .externalPrice(cjProduct.sellPrice())
                .externalStockStatus(ExternalStockStatus.IN_STOCK)
                .lastCheckedAt(Instant.now())
                .build();
        item.setTenantId(tenantId);

        return toDto(watchlistRepository.save(item));
    }

    public void removeFromWatchlist(UUID tenantId, UUID id) {
        MarketplaceWatchlistItem item = watchlistRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("WatchlistItem", id));
        watchlistRepository.delete(item);
    }

    private WatchlistItemDto toDto(MarketplaceWatchlistItem item) {
        return new WatchlistItemDto(
                item.getId(),
                item.getExternalProductId(),
                item.getExternalProductName(),
                item.getExternalImageUrl(),
                item.getExternalPrice(),
                item.getExternalStockStatus().name(),
                item.getLastCheckedAt(),
                item.getAddedAt()
        );
    }
}
