---
id: TASK-83.7
title: Tax zones + rules engine
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - tax
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поки один taxRate на Tenant. Потрібен tax engine: TaxZone (country/region), TaxRate (rate, inclusive/exclusive, categories), TaxExemption. EU VAT, US nexus, UA ПДВ (20%/7%/0%). Калькулятор на checkout.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: TaxZone, TaxRate, TaxCategory, TaxExemption
- [ ] #2 Inclusive vs exclusive pricing toggle на tenant
- [ ] #3 TaxCalculator.calculate(line_items, shipping_address) → per-line tax + total
- [ ] #4 UA default: 20% ПДВ, reduced 7%, exempt 0%
- [ ] #5 UI: /settings/taxes
<!-- AC:END -->
