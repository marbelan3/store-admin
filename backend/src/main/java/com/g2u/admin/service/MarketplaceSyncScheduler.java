package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.ConnectionStatus;
import com.g2u.admin.domain.marketplace.MarketplaceConnection;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketplaceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSyncScheduler.class);

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceSyncService syncService;
    private final MarketplaceOrderService orderService;

    public MarketplaceSyncScheduler(MarketplaceConnectionRepository connectionRepository,
                                     MarketplaceSyncService syncService,
                                     MarketplaceOrderService orderService) {
        this.connectionRepository = connectionRepository;
        this.syncService = syncService;
        this.orderService = orderService;
    }

    @Scheduled(cron = "${marketplace.sync.price.cron:0 0 */6 * * *}")
    public void priceSyncJob() {
        log.info("Starting price sync job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.syncPrices(conn);
            } catch (Exception e) {
                log.error("Price sync failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.stock.cron:0 0 */2 * * *}")
    public void stockSyncJob() {
        log.info("Starting stock sync job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.syncStock(conn);
            } catch (Exception e) {
                log.error("Stock sync failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.sku.cron:0 0 2 * * *}")
    public void skuValidationJob() {
        log.info("Starting SKU validation job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.validateSkus(conn);
            } catch (Exception e) {
                log.error("SKU validation failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.health.cron:0 0 4 * * *}")
    public void catalogHealthCheckJob() {
        log.info("Starting catalog health check job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.checkCatalogHealth(conn);
            } catch (Exception e) {
                log.error("Catalog health check failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.shipping.cron:0 0 3 * * 0}")
    public void shippingCacheRefreshJob() {
        log.info("Starting shipping cache refresh job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.refreshShippingCache(conn);
            } catch (Exception e) {
                log.error("Shipping cache refresh failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.watchlist.cron:0 0 6 * * *}")
    public void watchlistSyncJob() {
        log.info("Starting watchlist sync job");
        for (MarketplaceConnection conn : getActiveConnections()) {
            try {
                syncService.syncWatchlist(conn);
            } catch (Exception e) {
                log.error("Watchlist sync failed for connection {}: {}", conn.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${marketplace.sync.order-status.cron:0 0 */2 * * *}")
    public void orderStatusPollJob() {
        log.info("Starting order status poll job");
        try {
            orderService.pollOrderStatuses();
        } catch (Exception e) {
            log.error("Order status poll failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${marketplace.sync.tracking.cron:0 0 */4 * * *}")
    public void trackingPollJob() {
        log.info("Starting tracking poll job");
        try {
            orderService.pollTracking();
        } catch (Exception e) {
            log.error("Tracking poll failed: {}", e.getMessage());
        }
    }

    private List<MarketplaceConnection> getActiveConnections() {
        return connectionRepository.findBySyncEnabledTrueAndStatus(ConnectionStatus.ACTIVE);
    }
}
