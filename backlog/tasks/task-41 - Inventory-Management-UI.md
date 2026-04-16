---
id: TASK-41
title: Inventory Management UI
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:02'
labels:
  - backend
  - frontend
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поля trackInventory, quantity, lowStockThreshold існують на ProductVariant, але не використовуються. Потрібна окрема сторінка /inventory з таблицею SKU/варіант/кількість та можливістю масового оновлення.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Нова сторінка /inventory з таблицею всіх варіантів та їх залишків
- [x] #2 Можливість inline-редагування кількості
- [x] #3 Підсвітка рядків з низьким запасом (quantity <= lowStockThreshold)
- [x] #4 Масове оновлення кількості через CSV або форму
- [x] #5 Фільтр: показати тільки low stock / out of stock
- [x] #6 Бекенд ендпоінт GET /api/inventory та PATCH /api/inventory/bulk
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Inventory Management UI.

Backend: InventoryController (GET /api/inventory, PATCH /api/inventory/bulk), InventoryService combining product+variant inventory, lowStock/outOfStock filters, BulkInventoryUpdateRequest with validation. Migration 014 adds lowStockThreshold column.
Frontend: /inventory page with inline quantity editing, pending changes tracking, bulk save, low stock highlighting, filter buttons.
<!-- SECTION:FINAL_SUMMARY:END -->
