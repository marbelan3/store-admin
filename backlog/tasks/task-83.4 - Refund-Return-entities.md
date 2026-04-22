---
id: TASK-83.4
title: Refund + Return entities
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - orders
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Order.status має REFUNDED, але немає Refund/Return entity. Додати: Return (RMA), ReturnLine, Refund (amount, method, reason, transaction ref). Partial refunds. UI: Return creation flow, refund modal.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Return, ReturnLine, Refund з tenantId
- [ ] #2 API: POST /api/orders/{id}/returns, POST /api/orders/{id}/refunds
- [ ] #3 Partial refund: per-line amount
- [ ] #4 UI: Return flow на Order detail (reason codes, upload photos)
- [ ] #5 Order status автоматично оновлюється
<!-- AC:END -->
