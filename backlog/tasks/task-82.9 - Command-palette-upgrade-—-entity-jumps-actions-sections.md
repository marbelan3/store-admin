---
id: TASK-82.9
title: 'Command palette upgrade — entity jumps, actions, sections'
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux-pattern
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Розширити CommandPalette.svelte за межі router-ів: entity jumps (type "A3F" → Order #A3F21C), recent items, contextual actions ("Mark selected as Shipped", "Export CSV"), section headers Pages/Actions/Recent/Results. Shortcut hints біля пунктів.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Sections: Pages, Actions, Recent, Results
- [ ] #2 Fuzzy search по products/orders/customers через /search endpoint
- [ ] #3 Contextual actions змінюються залежно від current route
- [ ] #4 Keyboard shortcut hint (⌘+Shift+P) біля кожного пункту
- [ ] #5 Recent items persist in localStorage (10 items)
<!-- AC:END -->
