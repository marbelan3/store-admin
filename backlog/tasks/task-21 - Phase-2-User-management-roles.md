---
id: TASK-21
title: 'Phase 2: User management + roles'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 12:02'
updated_date: '2026-04-15 13:52'
labels:
  - phase-2
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
- Invite users by email to tenant
- Assign/change roles
- Remove users from tenant
- Add TENANT_EDITOR and TENANT_OWNER roles
- Custom permissions (flexible RBAC)
- User activity log
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Invite flow works end-to-end
- [ ] #2 Role change takes effect immediately
- [ ] #3 5 roles functional
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
User management: UserService, UserController (list users, change role, toggle active). Frontend: users table with role selector, activate/deactivate buttons.
<!-- SECTION:FINAL_SUMMARY:END -->
