---
id: TASK-82.3
title: 'Sidebar redesign — drop gradient, add section labels, left rail'
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - design-system
  - frontend
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити gradient-sidebar на чистий bg-card з 1px right border. Додати секції WORKSPACE/MARKETPLACE/CHANNELS/ADMIN як 11px uppercase muted labels. Active item: 2px primary left rail + subtle tint. Переглянути tooltip mode при collapse.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Видалено .gradient-sidebar, використовується --card
- [ ] #2 Section labels відображаються коли sidebar expanded
- [ ] #3 Active state: лівий 2px primary rail + bg-primary/5
- [ ] #4 Collapsed mode: tooltips зберігаються, іконки центровані
- [ ] #5 Працює у light + dark mode
<!-- AC:END -->
