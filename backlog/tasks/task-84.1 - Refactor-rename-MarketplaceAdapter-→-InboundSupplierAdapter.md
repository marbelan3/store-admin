---
id: TASK-84.1
title: 'Refactor: rename MarketplaceAdapter → InboundSupplierAdapter'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - refactor
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Щоб уникнути naming conflict з новим OutboundChannelAdapter. Поточний MarketplaceAdapter — CJ-shaped (інбаунд постачальник). Перемістити в infrastructure/sourcing/. Оновити всі імпорти. Не змінювати функціональність.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 MarketplaceAdapter → InboundSupplierAdapter (infrastructure/sourcing/)
- [ ] #2 CjDropshippingAdapter → CjInboundAdapter (infrastructure/sourcing/cj/)
- [ ] #3 Всі existing tests проходять без змін логіки
- [ ] #4 Controllers/services оновлені до нових імпортів
- [ ] #5 Existing marketplace інтеграція (CJ) працює без regression
<!-- AC:END -->
