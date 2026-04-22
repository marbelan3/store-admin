---
id: TASK-82.5
title: Unified Card primitive — kill border-l-4 accents
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - design-system
  - frontend
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Прибрати border-l-4 border-l-indigo-500 декорації з dashboard, products, inventory, orders. Застосувати ring + shadow language: ring-1 ring-foreground/10 + shadow-sm, hover lift -1px + shadow-md. Акценти через іконки/badge, не бордери.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Prop variant=accent=indigo|emerald|amber|rose видалено з Card
- [ ] #2 Dashboard stat cards показують колір через іконку+background circle, не border
- [ ] #3 Hover: translateY(-1px) + елевована тінь
- [ ] #4 Focus ring уніфікований через :focus-visible
<!-- AC:END -->
