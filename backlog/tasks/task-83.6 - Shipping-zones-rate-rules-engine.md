---
id: TASK-83.6
title: Shipping zones + rate rules engine
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - shipping
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поки shippingAmount — просто число на Order. Додати: ShippingZone (список країн/регіонів), ShippingRate (flat/by_weight/by_price_range/free_over), ShippingMethod. Storefront endpoint /shipping/quote. UI: zones matrix.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: ShippingZone, ShippingRate, ShippingMethod
- [ ] #2 Rate calculator: flat, weight-based (tiers), price-based (free-over-X)
- [ ] #3 Public endpoint GET /api/public/shipping/quote?country=&weight=&subtotal=
- [ ] #4 UI: /settings/shipping — зони з drag-and-drop сортуванням methods
- [ ] #5 Integration tests
<!-- AC:END -->
