---
id: TASK-84.4
title: 'ChannelCategoryMapping + ChannelInventoryAllocation — migrations 032, 034'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ChannelCategoryMapping: наша Category → зовнішня (id, path, attributesJson). ChannelInventoryAllocation: per-channel slice (variantId, channelId, allocated, reserved).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase 032-create-channel-category-mappings.yaml
- [ ] #2 Liquibase 034-create-channel-inventory-allocations.yaml
- [ ] #3 API: CRUD для mappings
- [ ] #4 InventoryAllocation unique (variant_id, channel_id)
- [ ] #5 Tests covering constraint violations
<!-- AC:END -->
