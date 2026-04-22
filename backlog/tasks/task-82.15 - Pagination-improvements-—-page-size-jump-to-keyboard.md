---
id: TASK-82.15
title: 'Pagination improvements — page size, jump-to, keyboard'
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - tables
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити примітивну Prev/Next на: page size selector (25/50/100/200), input "Page X" для jump, total на мобілі, клавіші ← → для нав. Пам'ятати pageSize per page в localStorage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 <Pagination> компонент з pageSize selector
- [ ] #2 Jump-to input (number type, validation)
- [ ] #3 Keyboard ← → змінюють сторінку коли focus не в input
- [ ] #4 pageSize per-page у localStorage (inventory-pageSize, products-pageSize)
- [ ] #5 Total показується на всіх розмірах екрана
<!-- AC:END -->
