---
id: TASK-82.7
title: 'Table enhancements — sticky header, density modes, zebra, column toggles'
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - tables
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Всі list-сторінки. Sticky <thead> під top bar (top-14). Density toggle comfortable|compact (py-3 vs py-1.5), збережено в localStorage. Optional zebra striping. Column visibility menu. Row hover + focus ring. Застосувати на products, orders, customers, marketplace.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Sticky headers працюють при scroll
- [ ] #2 Density toggle у toolbar кожної сторінки, state перзистентний
- [ ] #3 Column toggle menu з checkboxes
- [ ] #4 Zebra striping опціональний через prop
- [ ] #5 Keyboard navigation: J/K між рядками, Enter — open quick-peek
<!-- AC:END -->
