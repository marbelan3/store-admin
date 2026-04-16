---
id: TASK-45
title: Skeleton loading замість тексту Loading
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:03'
labels:
  - frontend
  - ux
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
На всіх сторінках стан завантаження — текст 'Loading...' без візуальної структури. Компонент skeleton встановлений в shadcn-svelte, але не використовується.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Dashboard — skeleton карточки під час завантаження
- [x] #2 Products — skeleton рядки таблиці
- [x] #3 Categories — skeleton дерево
- [x] #4 Users — skeleton рядки таблиці
- [x] #5 Settings — skeleton поля форми
- [x] #6 Product edit — skeleton форма
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Replaced all Loading text with skeleton states.

Used shadcn-svelte Skeleton component (animate-pulse bg-muted). Applied to: Layout (full sidebar+content skeleton), Dashboard (4 stat cards), Products (6 table rows), Product edit (form cards), Categories (list items), Users (table rows with avatar circles), Settings (form fields), Audit Log (table rows).
<!-- SECTION:FINAL_SUMMARY:END -->
