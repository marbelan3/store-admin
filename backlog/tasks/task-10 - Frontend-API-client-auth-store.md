---
id: TASK-10
title: 'Frontend: API client + auth store'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:51'
labels:
  - frontend
  - api
  - phase-1
dependencies:
  - TASK-2
  - TASK-7
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create SvelteKit API layer:

1. HTTP client (src/lib/api/client.ts):
   - Base URL from env
   - JWT token from cookie (httpOnly)
   - Auto-attach Authorization header
   - Response interceptor: handle 401 (refresh token), 403, 500
   - TypeScript response types

2. Auth store (src/lib/stores/auth.store.ts):
   - Current user state
   - Permissions list
   - hasPermission() helper
   - isAuthenticated derived

3. Tenant store (src/lib/stores/tenant.store.ts):
   - Current tenant
   - Available tenants list
   - switchTenant() action

4. Auth API (src/lib/api/auth.api.ts):
   - Google OAuth redirect
   - Token refresh
   - Get current user
   - Logout
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 API client handles JWT auth
- [ ] #2 401 triggers token refresh
- [ ] #3 Auth store tracks current user and permissions
- [ ] #4 Tenant store manages tenant context
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created API client with auto token refresh, products API module, auth store (Svelte 5 runes), TypeScript types for auth/products.
<!-- SECTION:FINAL_SUMMARY:END -->
