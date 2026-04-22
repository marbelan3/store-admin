package com.g2u.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2u.admin.domain.marketplace.*;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjProduct;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles per-product transactional sync work.
 * Each method runs in its own transaction so that a failure
 * in one product does not roll back the rest.
 */
@Service
public class MarketplaceSyncProcessor {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSyncProcessor.class);

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final MarketplacePriceHistoryRepository priceHistoryRepository;
    private final MarketplaceSyncLogRepository syncLogRepository;
    private final ProductRepository productRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public MarketplaceSyncProcessor(MarketplaceConnectionRepository connectionRepository,
                                     MarketplaceProductRepository marketplaceProductRepository,
                                     MarketplaceVariantMappingRepository variantMappingRepository,
                                     MarketplacePriceHistoryRepository priceHistoryRepository,
                                     MarketplaceSyncLogRepository syncLogRepository,
                                     ProductRepository productRepository,
                                     MarketplaceAdapter marketplaceAdapter,
                                     MarketplaceConnectionService connectionService,
                                     NotificationService notificationService,
                                     ObjectMapper objectMapper) {
        this.connectionRepository = connectionRepository;
        this.marketplaceProductRepository = marketplaceProductRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.syncLogRepository = syncLogRepository;
        this.productRepository = productRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    // --- Result record ---

    public record SyncItemResult(int checked, int updated, int errors, List<String> errorMessages) {
        static SyncItemResult skipped() {
            return new SyncItemResult(0, 0, 0, List.of());
        }
    }

    // --- Connection & product loading ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getTokenForConnection(UUID tenantId, UUID connectionId) {
        MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .orElseThrow();
        return connectionService.getValidAccessToken(connection);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<UUID> loadSyncableProductIds(UUID connectionId) {
        return marketplaceProductRepository.findSyncableByConnectionId(connectionId)
                .stream()
                .map(MarketplaceProduct::getId)
                .toList();
    }

    // --- Sync log management ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createSyncLog(UUID connectionId, SyncType syncType) {
        MarketplaceConnection connection = connectionRepository.findById(connectionId).orElseThrow();
        MarketplaceSyncLog syncLog = MarketplaceSyncLog.builder()
                .connection(connection)
                .syncType(syncType)
                .status("RUNNING")
                .build();
        return syncLogRepository.save(syncLog).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeSyncLog(UUID syncLogId, int checked, int updated, int errors, List<String> errorDetails) {
        MarketplaceSyncLog syncLog = syncLogRepository.findById(syncLogId).orElseThrow();
        syncLog.setItemsChecked(checked);
        syncLog.setItemsUpdated(updated);
        syncLog.setErrorsCount(errors);
        syncLog.setStatus(errors > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        syncLog.setCompletedAt(Instant.now());
        if (!errorDetails.isEmpty()) {
            try {
                syncLog.setErrorDetails(objectMapper.writeValueAsString(
                        errorDetails.size() > 100 ? errorDetails.subList(0, 100) : errorDetails));
            } catch (JsonProcessingException e) {
                syncLog.setErrorDetails("[]");
            }
        }
        syncLogRepository.save(syncLog);
        log.info("Sync {} completed: checked={}, updated={}, errors={}",
                syncLog.getSyncType(), checked, updated, errors);
    }

    // --- Per-product price sync ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncItemResult syncPriceForProduct(String token, UUID marketplaceProductId) {
        MarketplaceProduct mp = marketplaceProductRepository.findById(marketplaceProductId).orElse(null);
        if (mp == null || mp.isExcluded()) return SyncItemResult.skipped();

        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            CjProduct detail = marketplaceAdapter.getProductDetails(token, mp.getExternalProductId());
            List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());
            boolean priceChanged = false;

            for (MarketplaceVariantMapping mapping : mappings) {
                try {
                    BigDecimal newSourcePrice = findVariantPrice(detail, mapping.getCjVariantId());

                    if (newSourcePrice != null && mapping.getSourcePrice() != null
                            && newSourcePrice.compareTo(mapping.getSourcePrice()) != 0) {
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
                    errorMessages.add("Variant " + mapping.getCjSku() + ": " + e.getMessage());
                }
            }

            if (priceChanged) {
                recalculateMargin(mp, mappings);
                checkMarginAlert(mp);
                marketplaceProductRepository.save(mp);
                return new SyncItemResult(1, 1, errors, errorMessages);
            }
        } catch (Exception e) {
            errors++;
            errorMessages.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
        }

        return new SyncItemResult(1, 0, errors, errorMessages);
    }

    // --- Per-product stock sync ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncItemResult syncStockForProduct(String token, UUID marketplaceProductId) {
        MarketplaceProduct mp = marketplaceProductRepository.findById(marketplaceProductId).orElse(null);
        if (mp == null || mp.isExcluded()) return SyncItemResult.skipped();

        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());
            boolean stockChanged = false;
            int totalStock = 0;
            int successfulFetches = 0;

            for (MarketplaceVariantMapping mapping : mappings) {
                try {
                    int newStock = marketplaceAdapter.getStock(token, mapping.getCjVariantId());
                    successfulFetches++;
                    mapping.setStockLastCheckedAt(Instant.now());
                    if (mapping.getStockQuantity() == null || newStock != mapping.getStockQuantity()) {
                        mapping.setStockQuantity(newStock);
                        if (mapping.getVariant() != null) {
                            mapping.getVariant().setQuantity(newStock);
                        }
                        stockChanged = true;
                    }
                    variantMappingRepository.save(mapping);
                    totalStock += newStock;
                } catch (Exception e) {
                    errors++;
                    errorMessages.add("Variant " + mapping.getCjSku() + ": " + e.getMessage());
                    if (mapping.getStockQuantity() != null) {
                        totalStock += mapping.getStockQuantity();
                    }
                }
            }

            if (stockChanged) {
                mp.getProduct().setQuantity(totalStock);
                productRepository.save(mp.getProduct());
            }

            if (successfulFetches > 0) {
                checkLowStockAlert(mp, totalStock);
            }
            marketplaceProductRepository.save(mp);

            return new SyncItemResult(1, stockChanged ? 1 : 0, errors, errorMessages);
        } catch (Exception e) {
            errors++;
            errorMessages.add("Product " + mp.getExternalProductId() + ": " + e.getMessage());
            return new SyncItemResult(1, 0, errors, errorMessages);
        }
    }

    // --- Helpers ---

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
}
