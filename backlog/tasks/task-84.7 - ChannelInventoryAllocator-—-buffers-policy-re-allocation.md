---
id: TASK-84.7
title: 'ChannelInventoryAllocator — buffers, policy, re-allocation'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - inventory
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Pure domain service. Given variant on-hand + list of channel allocations → compute per-channel. Policies: EQUAL, WEIGHTED_BY_VELOCITY, PRIORITY. Global buffer %. Invoked on inventory change + nightly reconciliation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 ChannelInventoryAllocator.recomputeForVariant(variantId, policy)
- [ ] #2 3 policies: EQUAL, WEIGHTED_BY_VELOCITY (30d sales), PRIORITY (channel.priority)
- [ ] #3 Buffer: reserve X% unallocated (oversell protection)
- [ ] #4 Emits PublishJob(action=UPDATE_STOCK) для каналів з delta
- [ ] #5 Nightly cron ChannelInventoryReconcileJob
<!-- AC:END -->
