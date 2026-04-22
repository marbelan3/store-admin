---
id: TASK-83.11
title: Customer groups + segments + B2B price lists
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - customers
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Customer дуже простий. Додати: CustomerGroup (VIP, Wholesale), Segment (rule-based: "spent > 1000 last 30d"), PriceList (per-customer-group discount чи fixed price матриця).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: CustomerGroup, Segment (rule JSON), PriceList, PriceListEntry
- [ ] #2 Customer.groupIds: N:M
- [ ] #3 Segments компілюються в SQL через SegmentCompiler
- [ ] #4 PriceList застосовується в CartCalculator якщо customer в group
- [ ] #5 UI: /customers/groups, /customers/segments
<!-- AC:END -->
