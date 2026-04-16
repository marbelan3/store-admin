---
id: TASK-16
title: 'Frontend: PermissionGate + route guards'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 12:01'
updated_date: '2026-04-15 12:55'
labels:
  - frontend
  - security
  - phase-1
dependencies:
  - TASK-10
  - TASK-12
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create RBAC UI components:

1. PermissionGate.svelte (src/lib/components/PermissionGate.svelte):
   - Props: permission (string), fallback (optional snippet)
   - Renders children only if user has permission
   - Uses auth store hasPermission()

2. Route guard in +layout.server.ts:
   - Check auth cookie
   - Fetch user permissions from backend
   - Store in page data for client
   - Redirect to /login if not authenticated

3. Per-page guards in +page.server.ts:
   - Check specific permissions for routes
   - /users requires users:read
   - /settings requires settings:read

4. Sidebar filtering based on permissions
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PermissionGate hides content based on role
- [ ] #2 Route guards redirect unauthorized users
- [ ] #3 Sidebar shows only permitted nav items
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added PermissionGate component with role/requireEdit props and snippet-based children/fallback. Route guard in (app) layout redirects unauthenticated users.
<!-- SECTION:FINAL_SUMMARY:END -->
