package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.*;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.infrastructure.marketplace.CjApiException;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjProduct;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjVariant;
import com.g2u.admin.service.MarketplaceSyncProcessor.SyncItemResult;
import com.g2u.admin.web.dto.MarketplaceAlertDto;
import com.g2u.admin.web.dto.MarketplaceSyncLogDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MarketplaceSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSyncService.class);

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final MarketplaceSyncLogRepository syncLogRepository;
    private final MarketplaceWatchlistItemRepository watchlistRepository;
    private final ProductRepository productRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;
    private final NotificationService notificationService;
    private final MarketplaceSyncProcessor processor;

    public MarketplaceSyncService(MarketplaceConnectionRepository connectionRepository,
                                   MarketplaceProductRepository marketplaceProductRepository,
                                   MarketplaceVariantMappingRepository variantMappingRepository,
                                   MarketplaceSyncLogRepository syncLogRepository,
                                   MarketplaceWatchlistItemRepository watchlistRepository,
                                   ProductRepository productRepository,
                                   MarketplaceAdapter marketplaceAdapter,
                                   MarketplaceConnectionService connectionService,
                                   NotificationService notificationService,
                                   MarketplaceSyncProcessor processor) {
        this.connectionRepository = connectionRepository;
        this.marketplaceProductRepository = marketplaceProductRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.syncLogRepository = syncLogRepository;
        this.watchlistRepository = watchlistRepository;
        this.productRepository = productRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
        this.notificationService = notificationService;
        this.processor = processor;
    }

    // --- Price Sync (batch, per-product transactions) ---

    public void syncPrices(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        syncPricesInternal(token, connection.getId());
    }

    private void syncPricesInternal(String token, UUID connectionId) {
        List<UUID> productIds = processor.loadSyncableProductIds(connectionId);
        UUID syncLogId = processor.createSyncLog(connectionId, SyncType.PRICE);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (UUID productId : productIds) {
            SyncItemResult result = processor.syncPriceForProduct(token, productId);
            checked += result.checked();
            updated += result.updated();
            errors += result.errors();
            errorDetails.addAll(result.errorMessages());
        }

        processor.completeSyncLog(syncLogId, checked, updated, errors, errorDetails);
    }

    // --- Stock Sync (batch, per-product transactions) ---

    public void syncStock(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        syncStockInternal(token, connection.getId());
    }

    private void syncStockInternal(String token, UUID connectionId) {
        List<UUID> productIds = processor.loadSyncableProductIds(connectionId);
        UUID syncLogId = processor.createSyncLog(connectionId, SyncType.STOCK);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (UUID productId : productIds) {
            SyncItemResult result = processor.syncStockForProduct(token, productId);
            checked += result.checked();
            updated += result.updated();
            errors += result.errors();
            errorDetails.addAll(result.errorMessages());
        }

        processor.completeSyncLog(syncLogId, checked, updated, errors, errorDetails);
    }

    // --- SKU Validation ---

    @Transactional
    public void validateSkus(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        UUID syncLogId = processor.createSyncLog(connection.getId(), SyncType.SKU_VALIDATION);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            try {
                CjProduct detail = marketplaceAdapter.getProductDetails(token, mp.getExternalProductId());
                List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());

                for (MarketplaceVariantMapping mapping : mappings) {
                    checked++;
                    CjVariant found = findVariant(detail, mapping.getCjVariantId());

                    if (found == null) {
                        if (mapping.getSkuStatus() != SkuStatus.NOT_FOUND) {
                            mapping.setPreviousCjSku(mapping.getCjSku());
                            mapping.setSkuStatus(SkuStatus.NOT_FOUND);
                            variantMappingRepository.save(mapping);
                            updated++;

                            notificationService.createNotification(mp.getTenantId(), null,
                                    NotificationType.MARKETPLACE_SKU_CHANGED,
                                    "SKU not found: " + mapping.getCjSku(),
                                    "CJ variant " + mapping.getCjVariantId() + " for product " + mp.getProduct().getName() + " was not found");
                        }
                    } else if (!found.variantSku().equals(mapping.getCjSku())) {
                        mapping.setPreviousCjSku(mapping.getCjSku());
                        mapping.setCjSku(found.variantSku());
                        mapping.setSkuStatus(SkuStatus.CHANGED);
                        variantMappingRepository.save(mapping);
                        updated++;

                        notificationService.createNotification(mp.getTenantId(), null,
                                NotificationType.MARKETPLACE_SKU_CHANGED,
                                "SKU changed: " + mapping.getPreviousCjSku() + " → " + mapping.getCjSku(),
                                "CJ variant for product " + mp.getProduct().getName() + " has a new SKU");
                    } else if (mapping.getSkuStatus() != SkuStatus.ACTIVE) {
                        mapping.setSkuStatus(SkuStatus.ACTIVE);
                        variantMappingRepository.save(mapping);
                        updated++;
                    }
                }
            } catch (Exception e) {
                errors++;
                errorDetails.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
            }
        }

        processor.completeSyncLog(syncLogId, checked, updated, errors, errorDetails);
    }

    // --- Catalog Health Check ---

    @Transactional
    public void checkCatalogHealth(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        UUID syncLogId = processor.createSyncLog(connection.getId(), SyncType.CATALOG_HEALTH);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            checked++;
            try {
                marketplaceAdapter.getProductDetails(token, mp.getExternalProductId());
                if (mp.getSyncStatus() == SyncStatus.ERROR) {
                    mp.setSyncStatus(SyncStatus.SYNCED);
                    marketplaceProductRepository.save(mp);
                    updated++;
                }
            } catch (CjApiException e) {
                if (mp.getSyncStatus() != SyncStatus.DELISTED) {
                    mp.setSyncStatus(SyncStatus.DELISTED);
                    mp.getProduct().setStatus(ProductStatus.ARCHIVED);
                    productRepository.save(mp.getProduct());
                    marketplaceProductRepository.save(mp);
                    updated++;

                    notificationService.createNotification(mp.getTenantId(), null,
                            NotificationType.MARKETPLACE_PRODUCT_DELISTED,
                            "Product delisted: " + mp.getProduct().getName(),
                            "CJ product " + mp.getExternalProductId() + " is no longer available");
                }
            } catch (Exception e) {
                errors++;
                errorDetails.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
            }
        }

        processor.completeSyncLog(syncLogId, checked, updated, errors, errorDetails);
    }

    // --- Shipping Cache Refresh ---

    @Transactional
    public void refreshShippingCache(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        UUID syncLogId = processor.createSyncLog(connection.getId(), SyncType.TRACKING);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());

            for (MarketplaceVariantMapping mapping : mappings) {
                checked++;
                try {
                    BigDecimal shippingCost = marketplaceAdapter.calculateShipping(
                            token, mapping.getCjSku(),
                            mapping.getWarehouseId(), "UA", 1);

                    if (mapping.getShippingEstimate() == null
                            || shippingCost.compareTo(mapping.getShippingEstimate()) != 0) {
                        mapping.setShippingEstimate(shippingCost);
                        variantMappingRepository.save(mapping);
                        updated++;
                    }
                } catch (Exception e) {
                    errors++;
                    errorDetails.add("Variant " + mapping.getCjSku() + ": " + e.getMessage());
                }
            }
        }

        processor.completeSyncLog(syncLogId, checked, updated, errors, errorDetails);
    }

    // --- Watchlist Sync ---

    @Transactional
    public void syncWatchlist(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        var watchlistItems = watchlistRepository.findByTenantId(connection.getTenantId(), Pageable.unpaged());

        for (MarketplaceWatchlistItem item : watchlistItems) {
            try {
                CjProduct detail = marketplaceAdapter.getProductDetails(token, item.getExternalProductId());
                item.setExternalPrice(detail.sellPrice());
                item.setExternalProductName(detail.productName());
                item.setExternalImageUrl(detail.productImage());
                item.setLastCheckedAt(Instant.now());
                watchlistRepository.save(item);
            } catch (Exception e) {
                log.warn("Watchlist sync failed for {}: {}", item.getExternalProductId(), e.getMessage());
            }
        }
    }

    // --- Run All for a Connection (async) ---

    @Async
    public void runAllSyncs(UUID tenantId, UUID connectionId) {
        try {
            log.info("Starting async sync for connection {}", connectionId);
            String token = processor.getTokenForConnection(tenantId, connectionId);
            syncPricesInternal(token, connectionId);
            syncStockInternal(token, connectionId);
            log.info("Async sync completed for connection {}", connectionId);
        } catch (Exception e) {
            log.error("Async sync failed for connection {}: {}", connectionId, e.getMessage(), e);
        }
    }

    // --- Sync Logs ---

    @Transactional(readOnly = true)
    public Page<MarketplaceSyncLogDto> getSyncLogs(UUID tenantId, UUID connectionId, Pageable pageable) {
        connectionRepository.findByTenantIdAndId(tenantId, connectionId).orElseThrow();
        return syncLogRepository.findByConnectionIdOrderByStartedAtDesc(connectionId, pageable)
                .map(this::toSyncLogDto);
    }

    // --- Alerts ---

    @Transactional(readOnly = true)
    public List<MarketplaceAlertDto> getAlerts(UUID tenantId) {
        List<MarketplaceProduct> marginAlerts = marketplaceProductRepository.findMarginAlerts(tenantId);
        return marginAlerts.stream()
                .map(mp -> new MarketplaceAlertDto(
                        mp.getId(),
                        mp.getProduct().getId(),
                        mp.getProduct().getName(),
                        "MARGIN_VIOLATION",
                        "Margin " + mp.getCurrentMarginPct() + "% is below minimum " + mp.getMinMarginPct() + "%",
                        mp.getCurrentMarginPct(),
                        mp.getMinMarginPct()
                ))
                .toList();
    }

    // --- Internal Helpers ---

    private CjVariant findVariant(CjProduct product, String cjVariantId) {
        if (product.variants() != null) {
            for (CjVariant v : product.variants()) {
                if (v.vid().equals(cjVariantId)) {
                    return v;
                }
            }
        }
        return null;
    }

    private MarketplaceSyncLogDto toSyncLogDto(MarketplaceSyncLog l) {
        return new MarketplaceSyncLogDto(
                l.getId(), l.getConnection().getId(),
                l.getSyncType().name(), l.getStatus(),
                l.getItemsChecked(), l.getItemsUpdated(),
                l.getErrorsCount(), l.getErrorDetails(),
                l.getStartedAt(), l.getCompletedAt());
    }
}
