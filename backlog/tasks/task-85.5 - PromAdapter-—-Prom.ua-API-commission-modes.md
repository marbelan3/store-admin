---
id: TASK-85.5
title: PromAdapter — Prom.ua API + commission modes
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - channels
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prom.ua Marketplace API: products, orders, categories. Commission modes: per sale (varies by category). Feed + Orders via API. Prom categories depth up to 5.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PromCredentials (apiToken)
- [ ] #2 OutboundChannelAdapter impl: productCreate, productUpdate, stockUpdate
- [ ] #3 OrderIngestion: poll /orders/list every 15min
- [ ] #4 Category mapping: ours -> Prom (5 levels deep)
- [ ] #5 Commission display in UI (per category)
- [ ] #6 Tests with fixtures
<!-- AC:END -->
