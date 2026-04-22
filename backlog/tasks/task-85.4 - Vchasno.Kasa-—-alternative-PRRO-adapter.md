---
id: TASK-85.4
title: Vchasno.Kasa — alternative PRRO adapter
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - fiscalization
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Vchasno.Kasa - second PRRO provider. Same FiscalAdapter interface. Toggle per tenant. Failover logic: if primary down >5min - use secondary.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 FiscalAdapter interface (sendSale, sendReturn, getReceipt)
- [ ] #2 VchasnoKasaAdapter implementation
- [ ] #3 Tenant.primaryFiscalProvider + secondaryFiscalProvider
- [ ] #4 Failover logic in FiscalService
- [ ] #5 Tests with fixtures
<!-- AC:END -->
