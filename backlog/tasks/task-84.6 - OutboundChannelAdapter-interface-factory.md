---
id: TASK-84.6
title: OutboundChannelAdapter interface + factory
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
Interface OutboundChannelAdapter: type(), publishProduct(), updateListing(), updateInventory(bulk), updatePrice(bulk), delist(), parseWebhookOrder(), acknowledgeOrder(). Factory Map<ChannelType, OutboundChannelAdapter> wired by Spring.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Interface в infrastructure/saleschannel/OutboundChannelAdapter.java
- [ ] #2 DTOs: OutboundProduct, OutboundPriceUpdate, OutboundInventoryUpdate, InboundOrder, ListingResult
- [ ] #3 Factory OutboundChannelAdapterFactory з Map<ChannelType, Adapter>
- [ ] #4 NoOp default implementation для test
- [ ] #5 Всі існуючі adapters registered
<!-- AC:END -->
