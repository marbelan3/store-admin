---
id: TASK-84.9
title: 'Cycle prevention: ThreadLocal suppressOutboundPush'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - architecture
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Якщо webhook з каналу приносить ордер → stock decrement не має повертатися на цей канал як push (нескінчений loop). Analogous to TenantContext: ThreadLocal<Boolean> suppressOutboundPush тримає scope. InventoryService перевіряє прапорець.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 OutboundPushContext клас з set/clear/isSuppressed
- [ ] #2 ChannelOrderIngestionService ставить suppress для JERE channel_id ДО inventory change
- [ ] #3 ChannelPublishService.enqueue() skip якщо suppress == true для того ж channel
- [ ] #4 Інші канали продовжують отримувати updates (оновлені стоки)
- [ ] #5 Integration test: webhook → decrement → channel sync
<!-- AC:END -->
