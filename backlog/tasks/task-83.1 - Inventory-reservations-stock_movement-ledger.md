---
id: TASK-83.1
title: Inventory reservations + stock_movement ledger
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - inventory
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Додати резервації при створенні ордера (prevent oversell). Таблиця stock_movements (append-only ledger: type RECEIVE/SHIP/RESERVE/RELEASE/ADJUST, variant_id, quantity delta, reference (order/adjustment), actor). Decrement quantity транзакційно на order create.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase migration: stock_movements + reservations
- [ ] #2 OrderService.createOrder резервує inventory атомарно
- [ ] #3 Cancel/refund → release reservation
- [ ] #4 Endpoint GET /api/inventory/{variantId}/movements для історії
- [ ] #5 Unit + integration tests
<!-- AC:END -->
