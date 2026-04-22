---
id: TASK-85.9
title: FOP/TOV tenant tax regime + auto-PDV on invoices
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - frontend
  - ukraine
  - tax
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Tenant.taxRegime: FOP_3 (single tax 5%), FOP_2 (5%), TOV_ZAG (general), TOV_PDV (general with PDV 20%). Auto-calculate PDV on invoices + receipts. Display in Settings.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Tenant.taxRegime enum
- [ ] #2 TaxCalculator automatically applies correct rate based on regime
- [ ] #3 Invoice PDF shows PDV breakdown if TOV_PDV
- [ ] #4 UI: /settings/tax - choose regime with explanations (Ukrainian)
- [ ] #5 Validation: if regime = FOP_3 -> cap turnover warning
<!-- AC:END -->
