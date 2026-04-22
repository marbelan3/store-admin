---
id: TASK-82.12
title: Cross-page bulk selection + undo toast
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux-pattern
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Зберігати selection при зміні фільтрів/пагінації. Show persistent banner "N selected across X pages". Destructive actions (delete, archive) показують undo toast 10s замість confirm dialog. Toast dismiss → final commit.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 BulkSelectionStore (svelte store) зберігає selected IDs + last entity type
- [ ] #2 Banner показується коли selected > 0
- [ ] #3 Destructive actions: emit event + undoToast(10s), if dismissed → real DELETE
- [ ] #4 Applied to products, orders, customers, marketplace products
- [ ] #5 Selection clear при entity-type change
<!-- AC:END -->
