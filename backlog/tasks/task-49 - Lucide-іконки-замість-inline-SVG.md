---
id: TASK-49
title: Lucide іконки замість inline SVG
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - frontend
  - ux
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sidebar та Dashboard використовують інлайн SVG (60+ рядків). lucide-svelte встановлений, але не використовується. Також замінити нативний checkbox та select на shadcn компоненти.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Всі inline SVG в sidebar замінені на Lucide компоненти
- [ ] #2 Dashboard іконки замінені на Lucide
- [ ] #3 Нативний checkbox (trackInventory) замінений на Switch/Checkbox
- [ ] #4 Нативний select (батьківська категорія) замінений на shadcn Select
- [ ] #5 PermissionGate використовується замість повторюваних {#if auth.canEdit}
<!-- AC:END -->
