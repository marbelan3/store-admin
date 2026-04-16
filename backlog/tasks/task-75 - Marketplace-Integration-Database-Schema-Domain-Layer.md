---
id: TASK-75
title: 'Marketplace Integration: Database Schema & Domain Layer'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:46'
updated_date: '2026-04-16 05:59'
labels:
  - backend
  - marketplace
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Фундамент для marketplace інтеграції. 5 нових таблиць з урахуванням реальних пасток CJ Dropshipping: SKU-centric model, warehouse mismatch, price instability, stock unreliability.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Entity MarketplaceConnection (id, tenantId, provider, apiKey encrypted, accessToken, tokenExpiresAt, defaultWarehouseId, defaultShippingMethod, syncEnabled, status, lastConnectedAt)
- [x] #2 Entity MarketplaceProduct (id, tenantId, connectionId FK, productId FK, externalProductId, syncStatus, pricingRule MARGIN/FIXED_MARKUP/MANUAL, targetMarginPct, fixedMarkupAmount, minMarginPct, currentMarginPct, marginAlertTriggered)
- [x] #3 Entity MarketplaceVariantMapping — КРИТИЧНА: id, tenantId, marketplaceProductId FK, variantId FK, cjVariantId, cjSku, previousCjSku, fallbackCjSku, warehouseId, warehouseCountry, sourcePrice, shippingEstimate cached, stockQuantity, stockLastCheckedAt, skuStatus ACTIVE/CHANGED/DISCONTINUED/NOT_FOUND
- [x] #4 Entity MarketplacePriceHistory (variantMappingId FK, priceType PRODUCT/SHIPPING, oldPrice, newPrice, oldMarginPct, newMarginPct, detectedAt)
- [x] #5 Entity MarketplaceSyncLog (connectionId FK, syncType PRICE/STOCK/CATALOG_HEALTH/ORDER_STATUS/TRACKING/SKU_VALIDATION, status, itemsChecked, itemsUpdated, errorsCount, errorDetails JSON)
- [x] #6 Liquibase migration 025 for all 5 tables with FK, indexes, unique constraints
- [x] #7 Add source field (OWN/MARKETPLACE) to Product entity for fast filtering
- [x] #8 Enum types: MarketplaceProvider, PricingRule, SyncStatus, SkuStatus, SyncType
- [x] #9 Repositories for all 5 entities with tenant-filtered queries
- [x] #10 Unit tests for enum validations and entity constraints
- [x] #11 Entity MarketplaceWatchlistItem (id, tenantId, connectionId FK, externalProductId, externalProductName, externalImageUrl, externalPrice, externalStockStatus IN_STOCK/LOW/OOS, lastCheckedAt, addedAt) + migration
- [x] #12 Entity field: MarketplaceProduct.excluded (boolean, default false) — коли true, всі sync jobs пропускають цей товар
- [x] #13 Entity field: MarketplaceProduct.lowStockThreshold (integer) — tenant-configurable поріг для сповіщень про низький stock
- [x] #14 Entity field: MarketplaceProduct.stockAlertSent (boolean) — запобігає дублюванню notifications
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create enum types (MarketplaceProvider, PricingRule, SyncStatus, SkuStatus, SyncType, ProductSource)
2. Create 6 entities in domain.marketplace package
3. Add source field to Product entity
4. Add new NotificationType values
5. Create repositories with tenant-filtered queries
6. Liquibase migration 025
7. Register migration in master changelog
8. Run tests to verify
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Marketplace integration database foundation: 6 new tables + source field on products.

Changes:
- 8 enums: MarketplaceProvider, PricingRule, SyncStatus, SkuStatus, SyncType, PriceType, ConnectionStatus, ExternalStockStatus, ProductSource
- 6 entities: MarketplaceConnection, MarketplaceProduct (with excluded/lowStockThreshold/stockAlertSent), MarketplaceVariantMapping, MarketplacePriceHistory, MarketplaceSyncLog, MarketplaceWatchlistItem
- 6 repositories with tenant-filtered queries
- Product.source field (OWN/MARKETPLACE) for fast filtering
- 4 new NotificationType values for marketplace alerts
- Liquibase migration 025: 7 changesets creating 6 tables with FKs, indexes, unique constraints

Tests: all 165 tests pass, migration runs cleanly on Testcontainers MySQL
<!-- SECTION:FINAL_SUMMARY:END -->
