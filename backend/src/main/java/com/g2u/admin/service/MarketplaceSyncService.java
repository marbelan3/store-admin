package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.*;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.infrastructure.marketplace.CjApiException;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjProduct;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjVariant;
import com.g2u.admin.web.dto.MarketplaceAlertDto;
import com.g2u.admin.web.dto.MarketplaceSyncLogDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSyncService.class);

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final MarketplacePriceHistoryRepository priceHistoryRepository;
    private final MarketplaceSyncLogRepository syncLogRepository;
    private final MarketplaceWatchlistItemRepository watchlistRepository;
    private final ProductRepository productRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;
    private final NotificationService notificationService;

    public MarketplaceSyncService(MarketplaceConnectionRepository connectionRepository,
                                   MarketplaceProductRepository marketplaceProductRepository,
                                   MarketplaceVariantMappingRepository variantMappingRepository,
                                   MarketplacePriceHistoryRepository priceHistoryRepository,
                                   MarketplaceSyncLogRepository syncLogRepository,
                                   MarketplaceWatchlistItemRepository watchlistRepository,
                                   ProductRepository productRepository,
                                   MarketplaceAdapter marketplaceAdapter,
                                   MarketplaceConnectionService connectionService,
                                   NotificationService notificationService) {
        this.connectionRepository = connectionRepository;
        this.marketplaceProductRepository = marketplaceProductRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.syncLogRepository = syncLogRepository;
        this.watchlistRepository = watchlistRepository;
        this.productRepository = productRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
        this.notificationService = notificationService;
    }

    // --- Price Sync ---

    public void syncPrices(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        MarketplaceSyncLog syncLog = startSyncLog(connection, SyncType.PRICE);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            checked++;
            try {
                List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());
                boolean priceChanged = false;

                for (MarketplaceVariantMapping mapping : mappings) {
                    try {
                        CjProduct detail = marketplaceAdapter.getProductDetails(token, mp.getExternalProductId());
                        BigDecimal newSourcePrice = findVariantPrice(detail, mapping.getCjVariantId());

                        if (newSourcePrice != null && mapping.getSourcePrice() != null
                                && newSourcePrice.compareTo(mapping.getSourcePrice()) != 0) {
                            // Record price history
                            MarketplacePriceHistory history = MarketplacePriceHistory.builder()
                                    .variantMapping(mapping)
                                    .priceType(PriceType.PRODUCT)
                                    .oldPrice(mapping.getSourcePrice())
                                    .newPrice(newSourcePrice)
                                    .build();
                            priceHistoryRepository.save(history);

                            mapping.setSourcePrice(newSourcePrice);
                            variantMappingRepository.save(mapping);
                            priceChanged = true;
                        }
                    } catch (Exception e) {
                        errors++;
                        errorDetails.add("Variant " + mapping.getCjSku() + ": " + e.getMessage());
                    }
                }

                if (priceChanged) {
                    updated++;
                    recalculateMargin(mp, mappings);
                    checkMarginAlert(mp);
                    marketplaceProductRepository.save(mp);
                }
            } catch (Exception e) {
                errors++;
                errorDetails.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
            }
        }

        completeSyncLog(syncLog, checked, updated, errors, errorDetails);
    }

    // --- Stock Sync ---

    public void syncStock(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        MarketplaceSyncLog syncLog = startSyncLog(connection, SyncType.STOCK);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            checked++;
            try {
                List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());
                boolean stockChanged = false;
                int totalStock = 0;

                for (MarketplaceVariantMapping mapping : mappings) {
                    try {
                        int newStock = marketplaceAdapter.getStock(token, mapping.getCjVariantId());
                        if (mapping.getStockQuantity() == null || newStock != mapping.getStockQuantity()) {
                            mapping.setStockQuantity(newStock);
                            mapping.setStockLastCheckedAt(Instant.now());
                            variantMappingRepository.save(mapping);

                            // Update local variant quantity
                            if (mapping.getVariant() != null) {
                                mapping.getVariant().setQuantity(newStock);
                            }
                            stockChanged = true;
                        }
                        totalStock += newStock;
                    } catch (Exception e) {
                        errors++;
                        errorDetails.add("Variant " + mapping.getCjSku() + ": " + e.getMessage());
                    }
                }

                if (stockChanged) {
                    updated++;
                    // Update product total quantity
                    mp.getProduct().setQuantity(totalStock);
                    productRepository.save(mp.getProduct());
                }

                // Always check low stock alert during stock sync, even if stock didn't change
                checkLowStockAlert(mp, totalStock);
                marketplaceProductRepository.save(mp);
            } catch (Exception e) {
                errors++;
                errorDetails.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
            }
        }

        completeSyncLog(syncLog, checked, updated, errors, errorDetails);
    }

    // --- SKU Validation ---

    public void validateSkus(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        MarketplaceSyncLog syncLog = startSyncLog(connection, SyncType.SKU_VALIDATION);

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

        completeSyncLog(syncLog, checked, updated, errors, errorDetails);
    }

    // --- Catalog Health Check ---

    public void checkCatalogHealth(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        MarketplaceSyncLog syncLog = startSyncLog(connection, SyncType.CATALOG_HEALTH);

        int checked = 0, updated = 0, errors = 0;
        List<String> errorDetails = new ArrayList<>();

        for (MarketplaceProduct mp : products) {
            if (mp.isExcluded()) continue;
            checked++;
            try {
                marketplaceAdapter.getProductDetails(token, mp.getExternalProductId());
                // Product exists — mark as SYNCED if it was in error
                if (mp.getSyncStatus() == SyncStatus.ERROR) {
                    mp.setSyncStatus(SyncStatus.SYNCED);
                    marketplaceProductRepository.save(mp);
                    updated++;
                }
            } catch (CjApiException e) {
                // Product not found — mark as DELISTED
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

        completeSyncLog(syncLog, checked, updated, errors, errorDetails);
    }

    // --- Shipping Cache Refresh ---

    public void refreshShippingCache(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        List<MarketplaceProduct> products = marketplaceProductRepository.findSyncableByConnectionId(connection.getId());
        MarketplaceSyncLog syncLog = startSyncLog(connection, SyncType.TRACKING);

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

        completeSyncLog(syncLog, checked, updated, errors, errorDetails);
    }

    // --- Watchlist Sync ---

    public void syncWatchlist(MarketplaceConnection connection) {
        String token = connectionService.getValidAccessToken(connection);
        // Get all watchlist items for this connection's tenant
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

    // --- Run All for a Connection ---

    public void runAllSyncs(UUID tenantId, UUID connectionId) {
        MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .orElseThrow();
        syncPrices(connection);
        syncStock(connection);
    }

    // --- Sync Logs ---

    @Transactional(readOnly = true)
    public Page<MarketplaceSyncLogDto> getSyncLogs(UUID tenantId, UUID connectionId, Pageable pageable) {
        // Verify connection belongs to tenant
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

    private void recalculateMargin(MarketplaceProduct mp, List<MarketplaceVariantMapping> mappings) {
        BigDecimal totalSource = BigDecimal.ZERO;
        BigDecimal totalSelling = BigDecimal.ZERO;

        for (MarketplaceVariantMapping m : mappings) {
            if (m.getSourcePrice() != null && m.getVariant() != null && m.getVariant().getPrice() != null) {
                totalSource = totalSource.add(m.getSourcePrice());
                totalSelling = totalSelling.add(m.getVariant().getPrice());
            }
        }

        if (totalSelling.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = totalSelling.subtract(totalSource)
                    .divide(totalSelling, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            mp.setCurrentMarginPct(margin);
        }
    }

    private void checkMarginAlert(MarketplaceProduct mp) {
        if (mp.getMinMarginPct() != null && mp.getCurrentMarginPct() != null
                && mp.getCurrentMarginPct().compareTo(mp.getMinMarginPct()) < 0) {
            if (!mp.isMarginAlertTriggered()) {
                mp.setMarginAlertTriggered(true);
                notificationService.createNotification(mp.getTenantId(), null,
                        NotificationType.MARKETPLACE_MARGIN_ALERT,
                        "Low margin alert: " + mp.getProduct().getName(),
                        "Current margin " + mp.getCurrentMarginPct() + "% is below minimum " + mp.getMinMarginPct() + "%");
            }
        } else {
            mp.setMarginAlertTriggered(false);
        }
    }

    private void checkLowStockAlert(MarketplaceProduct mp, int totalStock) {
        if (mp.getLowStockThreshold() != null && totalStock <= mp.getLowStockThreshold()) {
            if (!mp.isStockAlertSent()) {
                mp.setStockAlertSent(true);
                notificationService.createNotification(mp.getTenantId(), null,
                        NotificationType.MARKETPLACE_LOW_STOCK,
                        "Low stock: " + mp.getProduct().getName(),
                        "Marketplace product stock is " + totalStock + " (threshold: " + mp.getLowStockThreshold() + ")");
            }
        } else {
            // Stock recovered above threshold — reset alert
            mp.setStockAlertSent(false);
        }
    }

    private BigDecimal findVariantPrice(CjProduct product, String cjVariantId) {
        if (product.variants() != null) {
            for (CjVariant v : product.variants()) {
                if (v.vid().equals(cjVariantId)) {
                    return v.variantSellPrice();
                }
            }
        }
        return product.sellPrice();
    }

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

    private MarketplaceSyncLog startSyncLog(MarketplaceConnection connection, SyncType syncType) {
        MarketplaceSyncLog syncLog = MarketplaceSyncLog.builder()
                .connection(connection)
                .syncType(syncType)
                .status("RUNNING")
                .build();
        return syncLogRepository.save(syncLog);
    }

    private void completeSyncLog(MarketplaceSyncLog syncLog, int checked, int updated, int errors, List<String> errorDetails) {
        syncLog.setItemsChecked(checked);
        syncLog.setItemsUpdated(updated);
        syncLog.setErrorsCount(errors);
        syncLog.setStatus(errors > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        syncLog.setCompletedAt(Instant.now());
        if (!errorDetails.isEmpty()) {
            syncLog.setErrorDetails("[" + String.join(",", errorDetails.stream()
                    .map(s -> "\"" + s.replace("\"", "\\\"") + "\"").toList()) + "]");
        }
        syncLogRepository.save(syncLog);
        log.info("Sync {} completed: checked={}, updated={}, errors={}",
                syncLog.getSyncType(), checked, updated, errors);
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
