---
id: TASK-85.11
title: Dropshipper flag + Rozetka publish warning
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - frontend
  - ukraine
  - channels
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Product.isDropship boolean (indicates product sources from CJ/etc, not own stock). Rozetka forbids dropshipping for most categories. Warning modal before publish to Rozetka. UI indicator (badge) in product list.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Product.isDropship migration + entity field
- [ ] #2 Auto-set to true for products imported from CJ
- [ ] #3 UI badge 'Dropship' in product list
- [ ] #4 Publish to Rozetka: if isDropship -> warning modal 'Rozetka may reject'
- [ ] #5 Admin override checkbox + audit log entry
<!-- AC:END -->
