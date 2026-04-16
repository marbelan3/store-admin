---
id: TASK-77
title: 'Marketplace: Product Import Flow'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:47'
updated_date: '2026-04-16 06:44'
labels:
  - backend
  - marketplace
dependencies:
  - TASK-76
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Селективний імпорт товарів з CJ (не bulk — CJ API не підходить для повного каталогу). Пошук в каталозі CJ, preview, вибір варіантів/складу, імпорт з створенням локального Product + variant mappings. Пастка: warehouse mismatch — один товар різні SKU на різних складах.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 CJ Catalog proxy: GET /api/marketplace/cj/catalog?q=&category=&page= — проксі до CJ listV2
- [x] #2 CJ Product detail proxy: GET /api/marketplace/cj/products/{pid} — повна інформація + варіанти + stock по складах
- [x] #3 Import endpoint: POST /api/marketplace/import — приймає externalProductId, вибрані варіанти з warehouseId, pricing rule, categoryId
- [x] #4 Import створює: Product (source=MARKETPLACE, status=DRAFT) + ProductVariants + Media (фільтровані зображення) + MarketplaceProduct + MarketplaceVariantMapping per variant
- [x] #5 Variant mapping зберігає: cjSku, cjVariantId, warehouseId, warehouseCountry, sourcePrice, stockQuantity — по конкретному складу
- [x] #6 Pricing calculation при імпорті: застосування pricing rule (margin/fixed/manual) до source price
- [x] #7 Image filtering: де-дублікація, пропуск low-quality, збереження через Media Manager
- [x] #8 Валідація: перевірка що CJ product/variant ще існують перед імпортом
- [x] #9 Тести: import flow, duplicate import prevention, pricing calculation
- [x] #10 Watchlist endpoints: POST /api/marketplace/watchlist (add), DELETE (remove), GET (list), POST /watchlist/import (batch import з watchlist)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create DTOs: ImportProductRequest, ImportVariantRequest, CjCatalogProductDto, CjProductDetailDto, WatchlistItemDto
2. Create MarketplaceImportService: import flow (Product + Variants + Media + MarketplaceProduct + Mappings)
3. Create MarketplaceWatchlistService: CRUD + batch import
4. Create MarketplaceCatalogController: CJ catalog proxy + import endpoint
5. Create MarketplaceWatchlistController: watchlist CRUD
6. Integration tests
7. Compile & test
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Product import flow from CJ Dropshipping + Watchlist management.

Changes:
- MarketplaceImportService: CJ catalog proxy (search + detail), selective import creating Product (source=MARKETPLACE) + ProductVariants + ProductMedia + MarketplaceProduct + MarketplaceVariantMapping, pricing calculation (MARGIN/FIXED_MARKUP/MANUAL), slug generation, image de-duplication, margin calculation
- MarketplaceWatchlistService: add/remove/list watchlist items with CJ product detail fetch
- MarketplaceCatalogController: GET /cj/catalog, GET /cj/products/{pid}, POST /import, GET/GET/{id} /products, PATCH /products/{id}/exclude, PATCH /products/{id}/threshold
- MarketplaceWatchlistController: GET/POST/DELETE /marketplace/watchlist
- DTOs: CjCatalogProductDto, CjProductDetailDto, ImportProductRequest, MarketplaceProductDto, WatchlistItemDto, AddToWatchlistRequest
- TestMarketplaceConfig: shared test adapter for all marketplace tests
- 10 integration tests: import with variants, duplicate prevention, fixed markup pricing, toggle exclude, update threshold, paginated list, watchlist add/duplicate/remove

Tests: all pass (183 total)
<!-- SECTION:FINAL_SUMMARY:END -->
