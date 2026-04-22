---
id: TASK-82.16
title: Per-entity activity timeline sidebar
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - backend
  - audit
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ActivityTimeline.svelte вже існує — розкрити її на product/order/customer detail pages. Права панель: всі події (created, updated, status-changed, imported, published), user, час. Фільтр по типу події. API endpoint /api/audit/entity/{type}/{id}.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Endpoint GET /api/audit/entity/{type}/{id}?since=&limit= повертає події
- [ ] #2 Product/Order/Customer detail pages мають <ActivityTimelineSidebar />
- [ ] #3 Timeline показує user avatar + action + diff + timestamp
- [ ] #4 Filter by event type (status change, edit, publish)
- [ ] #5 Дані беруться з існуючого AuditLog
<!-- AC:END -->
