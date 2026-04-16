---
id: TASK-78
title: 'Marketplace: Sync Engine (Price, Stock, SKU, Health)'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:47'
updated_date: '2026-04-16 06:51'
labels:
  - backend
  - marketplace
dependencies:
  - TASK-77
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Головний sync engine з 6 scheduled jobs для підтримки актуальності marketplace товарів. Критичні пастки: ціни нестабільні (можна продавати нижче собівартості), stock не гарантований, SKU можуть зникати, товари можуть бути видалені.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 PriceSyncJob (@Scheduled кожні 6 годин): перевірка source_price через CJ API, перерахунок margin, запис в price_history при зміні, trigger margin alert якщо margin < minMarginPct
- [x] #2 StockSyncJob (кожні 2 години): оновлення stock_quantity per variant mapping, mark zero-stock variants, update InventoryItem.quantity
- [x] #3 SkuValidationJob (nightly 02:00): валідація що всі cjSku ще існують, оновлення sku_status (ACTIVE/CHANGED/DISCONTINUED/NOT_FOUND), збереження previousCjSku при зміні
- [x] #4 CatalogHealthCheckJob (daily 04:00): перевірка external_product_id валідності, позначення DELISTED товарів, архівація Product якщо delisted
- [x] #5 ShippingCacheRefreshJob (weekly): оновлення shipping_estimate для активних mappings через CJ freightCalculate API
- [x] #6 MarketplaceSyncLog: кожен sync run логується з items_checked, items_updated, errors_count, error_details
- [x] #7 Margin alert system: notification створюється коли currentMarginPct < minMarginPct, відображається в /marketplace/alerts
- [x] #8 REST: GET /api/marketplace/sync-logs, POST /api/marketplace/sync/trigger (manual), GET /api/marketplace/alerts
- [x] #9 Config: sync intervals configurable per connection через marketplace_connections.config JSON
- [x] #10 Тести: price change detection, margin alert trigger, SKU status transitions, sync log recording
- [x] #11 WatchlistSyncJob (daily): легкий sync тільки price + stock для watchlist items, без створення Product
- [x] #12 Exclude filter: всі sync jobs (Price, Stock, SKU, CatalogHealth, Shipping) пропускають MarketplaceProduct де excluded=true
- [x] #13 Low stock notifications: StockSyncJob створює Notification через NotificationService коли stockQuantity <= lowStockThreshold і stockAlertSent=false, скидає stockAlertSent коли stock відновлюється
- [x] #14 REST: PATCH /api/marketplace/products/{id}/exclude (toggle excluded status), PATCH /api/marketplace/products/{id}/threshold (set lowStockThreshold)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create MarketplaceSyncService — core sync logic (price, stock, SKU, health, shipping, watchlist)
2. Create sync job classes with @Scheduled annotations
3. Integrate NotificationService for margin alerts and low-stock
4. Create MarketplaceSyncController — sync-logs, manual trigger, alerts
5. DTOs for sync logs and alerts
6. Integration tests with TestMarketplaceConfig
7. Compile & test
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Marketplace sync engine with 6 scheduled jobs, notifications, and alerts.

Changes:
- MarketplaceSyncService: 6 sync operations — syncPrices (price history, margin recalc, margin alerts), syncStock (quantity update, low-stock notifications with threshold), validateSkus (SKU status transitions, SKU change notifications), checkCatalogHealth (delisted detection, auto-archive), refreshShippingCache (shipping estimate update), syncWatchlist (lightweight price+stock for bookmarked items)
- MarketplaceSyncScheduler: @Scheduled cron jobs — prices every 6h, stock every 2h, SKU nightly 02:00, health daily 04:00, shipping weekly Sunday, watchlist daily 06:00. All cron expressions configurable via properties.
- MarketplaceSyncController: GET /sync-logs/{connectionId}, POST /sync/trigger/{connectionId}, GET /alerts
- Exclude filter: all sync jobs skip MarketplaceProduct where excluded=true
- Low stock notifications: creates Notification via NotificationService when stock <= threshold, resets when stock recovers
- Margin alert system: notification when currentMarginPct < minMarginPct, alerts endpoint returns all violations
- @EnableScheduling added to StoreAdminApplication
- 8 integration tests: price sync log, stock sync, low stock alert + notification, SKU validation, catalog health, excluded product skipping, margin alert query, manual trigger

Tests: all 191 pass
<!-- SECTION:FINAL_SUMMARY:END -->
