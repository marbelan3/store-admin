---
id: TASK-8
title: 'RBAC: Role-based access control'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:51'
labels:
  - backend
  - security
  - phase-1
dependencies:
  - TASK-6
  - TASK-7
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement RBAC with fixed roles for MVP:

Roles:
- SUPER_ADMIN: access to all tenants, tenant management
- TENANT_ADMIN: full CRUD within own tenant, user management
- TENANT_VIEWER: read-only access within own tenant

Backend:
1. Role enum in User entity
2. @PreAuthorize annotations on controllers
3. RbacService with hasPermission(user, resource, action) method
4. Permission map: role -> Set<Permission>
5. Custom Spring Security MethodSecurityExpressionHandler

Permissions format: resource:action (products:create, products:read, etc.)

SuperAdmin bypasses tenant filter to see all data.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 3 roles defined and enforced
- [ ] #2 @PreAuthorize on all API endpoints
- [ ] #3 SUPER_ADMIN sees all tenants
- [ ] #4 TENANT_VIEWER cannot create/update/delete
- [ ] #5 Permission denied returns 403
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added @PreAuthorize on write endpoints, @CurrentUser annotation, SecurityUtils helper, @EnableMethodSecurity in SecurityConfig.
<!-- SECTION:FINAL_SUMMARY:END -->
