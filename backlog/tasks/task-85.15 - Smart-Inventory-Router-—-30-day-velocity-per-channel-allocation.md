---
id: TASK-85.15
title: Smart Inventory Router — 30-day velocity + per-channel allocation
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - ai
  - inventory
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
AI-light: compute 30-day sales velocity per variant per channel. Re-allocate stock: more allocate to channels with higher velocity. Weekly job + manual trigger. Replaces naive EQUAL split.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Velocity calculator: sales_last_30d per (variant, channel)
- [ ] #2 ChannelInventoryAllocator policy WEIGHTED_BY_VELOCITY recomputes weekly (cron Monday 3am)
- [ ] #3 Floor per channel: min 1 unit if variant active on channel
- [ ] #4 UI: /channels/inventory/routing - shows current velocity + suggested alloc
- [ ] #5 Manual trigger button
- [ ] #6 Alert when variant runs low on high-velocity channel
<!-- AC:END -->
