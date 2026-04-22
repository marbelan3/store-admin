---
id: TASK-83.9
title: Custom-role permissions v1
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - rbac
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити фіксовані ролі (SUPER_ADMIN/TENANT_ADMIN/TENANT_VIEWER) на Permission enum + role_permissions join. Приклади permissions: product.read/write, order.refund, inventory.update, channel.publish. Default ролі залишаються, plus custom roles.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Permission enum (50+ permissions: <entity>.<action>)
- [ ] #2 Entities: Role, RolePermission (N:M)
- [ ] #3 Backfill: TENANT_ADMIN → all permissions, TENANT_VIEWER → *.read
- [ ] #4 @PreAuthorize("hasPermission('product.write')") на controllers
- [ ] #5 UI: /users/roles — CRUD custom roles + permission matrix
- [ ] #6 Migration не ламає існуючу auth
<!-- AC:END -->
