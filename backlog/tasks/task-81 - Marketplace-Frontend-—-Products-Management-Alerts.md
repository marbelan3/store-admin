---
id: TASK-81
title: 'Marketplace: Frontend — Products Management & Alerts'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:47'
updated_date: '2026-04-16 07:37'
labels:
  - frontend
  - marketplace
dependencies:
  - TASK-80
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
UI для управління імпортованими marketplace товарами: sync status, margin health, price history, SKU mappings, alerts dashboard.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Сторінка /marketplace/products: таблиця імпортованих товарів з колонками Name, Source (CJ badge), Status, Our Price vs CJ Price, Margin %, Sync Status, Last Synced
- [x] #2 Margin health indicators: green (>20%), yellow (10-20%), red (<10%) на кожному товарі
- [x] #3 Product detail /marketplace/products/[id]: variant mappings table з cjSku, warehouse badge, stock, source price, our price, margin
- [x] #4 Price history chart per variant (mini chart показує зміни CJ ціни за останній місяць)
- [x] #5 Pricing rule editor: зміна pricing rule inline, перерахунок ціни в реальному часі
- [x] #6 Сторінка /marketplace/alerts: dashboard з margin violations, SKU changes, OOS events, delisted products — color-coded badges
- [x] #7 Sync logs сторінка /marketplace/sync-logs: історія sync runs з drill-down по помилках
- [x] #8 Filter products list: по source (OWN/MARKETPLACE) на існуючій /products сторінці
- [x] #9 MarketplaceBadge component: показує CJ/AliExpress badge на product cards в загальному списку товарів
- [x] #10 Quantity control column: показувати поточний stock per variant mapping з кольоровим індикатором (green/yellow/red відносно lowStockThreshold)
- [x] #11 Exclude toggle: кнопка/switch на кожному marketplace product для exclude/include з підтвердженням, excluded товари відображаються сіро з badge 'Excluded'
- [x] #12 Stock threshold settings: inline-editable поле lowStockThreshold per marketplace product (default з tenant settings)
- [x] #13 Low stock alerts: інтеграція з existing notification bell — badge count включає marketplace low-stock notifications
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Backend: Add source field to ProductListDto + update JPQL queries + ProductSpecification + controller filter param
2. Frontend: Add source to ProductListItem type, add source filter on /products page, MarketplaceBadge component
3. Frontend: Create /marketplace/products page — table with margin indicators, exclude toggle, threshold editor
4. Frontend: Create /marketplace/products/[id] page — variant mappings table, pricing rule editor
5. Frontend: Create /marketplace/alerts page — alerts dashboard with color-coded badges
6. Frontend: Create /marketplace/sync-logs page — sync history with error drill-down
7. Add Marketplace sub-nav items to sidebar
8. Verify notification bell already shows marketplace alerts (AC #13)
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Marketplace Products Management & Alerts frontend with backend source filter support.

Backend changes:
- `ProductListDto` — added `source` field (OWN/MARKETPLACE) with `@JsonIgnore` + `source()` accessor
- `ProductRepository` — updated JPQL queries to include `p.source` in projections
- `ProductSpecification` — added `source` filter parameter to `buildFilter()`
- `ProductService.getProducts()` — accepts optional `ProductSource source` param
- `ProductController.listProducts()` — accepts `?source=MARKETPLACE` query param
- `MarketplaceCatalogController` — added `PATCH /products/{id}/pricing` and `GET /variants/{mappingId}/price-history` endpoints
- `MarketplaceImportService.updatePricing()` — updates pricing rule, margins on marketplace products
- `PriceHistoryDto` — new DTO for price history API

Frontend changes:
- `types/marketplace.ts` — added `PriceHistory` interface
- `api/marketplace.ts` — added `updatePricing()` and `getPriceHistory()` functions
- `types/product.ts` — added `source` to `ProductListItem`
- `api/products.ts` — added `source` to `ProductFilters`
- `/products` page — added Source filter dropdown (All/Own/Marketplace) + CJ badge on marketplace products
- Layout sidebar — added MP Products, Alerts, Sync Logs nav items with icons
- `/marketplace/products` — table with name, sync status, CJ price, margin % (green/yellow/red indicators), stock with color, inline threshold editor, exclude toggle
- `/marketplace/products/[id]` — summary cards (pricing rule editor, margin, sync status, variants), controls (exclude toggle, threshold), variant mappings table with expandable price history (mini SVG chart + recent changes list), inline pricing rule editor
- `/marketplace/alerts` — dashboard with summary count badges, color-coded alert cards (margin/SKU/OOS/delisted)
- `/marketplace/sync-logs` — connection selector, sync log table with type badges, status, error drill-down dialog, trigger sync button

All 196 backend tests pass. Frontend builds cleanly.
<!-- SECTION:FINAL_SUMMARY:END -->
