---
id: TASK-80
title: 'Marketplace: Frontend — Connections & Import UI'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:47'
updated_date: '2026-04-16 07:13'
labels:
  - frontend
  - marketplace
dependencies:
  - TASK-77
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Frontend для підключення до маркетплейсів та імпорту товарів. Пошук в CJ каталозі, preview товару з варіантами по складах, вибір складу, налаштування pricing rule, імпорт.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Сторінка /marketplace/connections: список підключень, статус, Add Connection dialog (provider select, API key input, test connection button)
- [x] #2 Сторінка /marketplace/import: пошук CJ каталогу (search input, category filter, price range), результати як картки з фото/ціною/рейтингом
- [x] #3 Product preview dialog: повна інформація CJ товару, варіанти з цінами, stock по різних складах (China/US/EU), вибір потрібних варіантів + складу
- [x] #4 Import dialog: pricing rule selector (Margin %/Fixed markup/Manual), target margin input, category select, preview розрахованої ціни
- [x] #5 Warehouse selector: показувати складський badge (🇨🇳 CN, 🇺🇸 US, 🇪🇺 EU) з shipping estimate та delivery time
- [x] #6 Sidebar navigation: додати Marketplace розділ з іконкою Store
- [x] #7 Empty state та loading для всіх сторінок
- [x] #8 Watchlist функціонал: bookmark/зірочка на товарах в каталозі для збереження без імпорту
- [x] #9 Сторінка /marketplace/watchlist: список збережених товарів з ціною, stock status, кнопка Import
- [x] #10 Batch import dialog: global pricing rule + per-product warehouse/variant configuration, preview розрахованої ціни
- [x] #11 Чекбокс-selection в каталозі + floating action bar Import Selected (аналогічно bulk actions на products)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create TypeScript types for marketplace entities (types/marketplace.ts)
2. Create marketplace API client (api/marketplace.ts)
3. Add Marketplace section to sidebar navigation with Store/Globe icon
4. Create /marketplace/connections page - list, create, edit, delete, test connections
5. Create /marketplace/import page - CJ catalog search, product preview, import dialog with warehouse selector
6. Create /marketplace/watchlist page - browsing, add-to-watchlist, remove, import from watchlist
7. Wire up batch import with checkbox selection on import page
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation complete:
- types/marketplace.ts: 14 interfaces covering connections, catalog, import, watchlist, sync, alerts, orders
- api/marketplace.ts: 16 API functions for all marketplace endpoints
- Layout: sidebar Marketplace section with Globe/Download/Eye icons and separator dividers
- /marketplace/connections: card grid with CRUD, test, sync trigger, sync logs dialog
- /marketplace/import: CJ catalog search, product cards with checkboxes, detail/import dialog (variant selection, warehouse badges, pricing rules), batch import dialog with progress bar
- /marketplace/watchlist: product grid with stock badges, remove confirmation, pagination
- All pages have loading skeletons and empty states
- Frontend builds cleanly
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added complete Marketplace frontend: Connections, Import, and Watchlist pages.

Changes:
- `frontend/src/lib/types/marketplace.ts` — 14 TypeScript interfaces matching all backend DTOs
- `frontend/src/lib/api/marketplace.ts` — 16 API client functions for connections, catalog, import, watchlist, sync, orders
- `frontend/src/routes/(app)/+layout.svelte` — Added Marketplace section to sidebar nav with Globe/Download/Eye icons, separator support
- `frontend/src/routes/(app)/marketplace/connections/+page.svelte` — Connection management: card grid, create/edit/delete dialogs, test connection, trigger sync, sync logs viewer
- `frontend/src/routes/(app)/marketplace/import/+page.svelte` — CJ catalog search, product cards with batch selection checkboxes, product detail dialog with variant selection and warehouse badges, pricing rule configuration (Margin/Fixed/Manual), batch import dialog with per-batch settings and progress bar
- `frontend/src/routes/(app)/marketplace/watchlist/+page.svelte` — Watchlist grid with stock status badges, remove with confirmation, pagination, link to import page

All pages include loading skeletons, empty states, and toast notifications. Frontend builds cleanly.
<!-- SECTION:FINAL_SUMMARY:END -->
