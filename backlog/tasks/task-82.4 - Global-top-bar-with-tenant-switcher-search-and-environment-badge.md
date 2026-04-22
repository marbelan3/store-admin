---
id: TASK-82.4
title: 'Global top bar with tenant switcher, search and environment badge'
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - design-system
  - frontend
  - multi-tenant
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Додати постійний top bar на десктопі: логотип/tenant switcher зліва, global search (відкриває command palette) в центрі, environment badge (Prod/Staging) + notifications + user menu справа. Зараз нічого з цього не існує на десктопі.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Top bar висотою 56px, sticky top-0
- [ ] #2 Tenant switcher popover (searchable) коли multi-tenant mode активний
- [ ] #3 Search trigger показує ⌘K shortcut, відкриває CommandPalette
- [ ] #4 Environment badge читається з VITE_ENV
- [ ] #5 Notifications bell + user avatar menu переїжджають з sidebar
<!-- AC:END -->
