---
id: TASK-83.2
title: Multi-location inventory (Location + InventoryLevel)
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - inventory
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поточний Product.quantity — одне поле. Додати: Location entity (warehouse, store, 3PL) + InventoryLevel (variant × location × available/reserved). Default 1 location per tenant при міграції. UI: управління локаціями, per-location stock.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Location, InventoryLevel з tenantId
- [ ] #2 Liquibase міграція + backfill default location з existing quantity
- [ ] #3 API: /api/locations CRUD, /api/inventory/{variantId}/levels
- [ ] #4 Frontend: /settings/locations page
- [ ] #5 Inventory list показує locations окремо
<!-- AC:END -->
