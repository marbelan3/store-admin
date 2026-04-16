---
id: TASK-12
title: 'Frontend: Admin dashboard layout'
status: Done
assignee: []
created_date: '2026-04-15 11:58'
updated_date: '2026-04-15 12:51'
labels:
  - frontend
  - ui
  - layout
  - phase-1
dependencies:
  - TASK-10
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create authenticated layout (src/routes/(app)/+layout.svelte):

Design: shadcn-svelte sidebar pattern (best practice admin dashboard)

1. Collapsible sidebar:
   - Logo/brand at top
   - Navigation groups (accordion): Dashboard, Products, Categories, Users, Settings
   - Each nav item: icon (Lucide) + label
   - Collapsed mode: icon-only
   - Active route highlighting
   - Permission-based: hide items user cannot access

2. Top header bar:
   - Breadcrumbs
   - Tenant switcher (dropdown with current tenant name)
   - User avatar + dropdown (profile, logout)
   - Dark/light mode toggle (mode-watcher)

3. Main content area with proper spacing

4. Mobile: sidebar as Sheet (slide-out drawer)

5. Auth guard in +layout.server.ts: redirect to /login if not authenticated

Best practice patterns from Svelte admin templates.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Collapsible sidebar with navigation groups
- [ ] #2 Tenant switcher in header
- [ ] #3 User menu with logout
- [ ] #4 Dark/light mode toggle
- [ ] #5 Breadcrumbs
- [ ] #6 Mobile responsive (Sheet sidebar)
- [ ] #7 Auth guard redirects to login
- [ ] #8 Permission-based nav items
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
App layout with collapsible sidebar, nav items, avatar dropdown, role-based nav visibility.
<!-- SECTION:FINAL_SUMMARY:END -->
