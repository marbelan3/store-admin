---
id: TASK-85.14
title: Unified Margin Optimizer — price suggestion engine
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - frontend
  - pricing
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
For each (variant, channel) compute: landed cost (CJ supplier price + shipping + duty) -> channel commission -> desired margin -> suggested list price. Alert if current price below floor. Recommendations on dashboard.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Landed cost formula: supplier_price + shipping_cost + import_duty + payment_fee
- [ ] #2 Per-channel commission config (Rozetka 10-25%, Shopify transaction 2%, eBay 12%)
- [ ] #3 Margin target per tenant (default 30%)
- [ ] #4 Suggested price = landed_cost / (1 - commission - target_margin)
- [ ] #5 Margin Alert: if current price gives <15% margin -> dashboard warning
- [ ] #6 Bulk price update action: 'Apply suggested to N products'
- [ ] #7 UI: Price Optimizer page with table sorted by margin asc
<!-- AC:END -->
