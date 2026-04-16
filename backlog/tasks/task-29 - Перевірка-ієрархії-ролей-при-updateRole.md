---
id: TASK-29
title: Перевірка ієрархії ролей при updateRole
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:28'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TENANT_ADMIN може призначити іншому користувачу роль SUPER_ADMIN, що вища за його власну. Потрібна бізнес-логіка: TENANT_ADMIN може призначати тільки TENANT_ADMIN та TENANT_VIEWER.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 TENANT_ADMIN не може призначити роль SUPER_ADMIN
- [ ] #2 SUPER_ADMIN може призначити будь-яку роль
- [ ] #3 Спроба ескалації привілеїв повертає 403 з зрозумілим повідомленням
- [ ] #4 Написано тест для перевірки ієрархії ролей
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added role hierarchy validation in UserService.updateRole: TENANT_ADMIN cannot assign SUPER_ADMIN, cannot modify SUPER_ADMIN users, cannot change own role. Updated UserController to pass caller info.
<!-- SECTION:FINAL_SUMMARY:END -->
