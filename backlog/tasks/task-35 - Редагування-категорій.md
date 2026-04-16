---
id: TASK-35
title: Редагування категорій
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:25'
labels:
  - frontend
  - ux
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Зараз категорії можна тільки створити або видалити. Немає UI для перейменування, зміни опису, переключення active, зміни батьківської категорії чи sortOrder. API updateCategory вже існує.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Клік на категорію відкриває панель/діалог редагування
- [ ] #2 Можна змінити назву, опис, slug, active toggle
- [ ] #3 Можна змінити батьківську категорію
- [ ] #4 Можна змінити sortOrder або drag-and-drop для порядку
- [ ] #5 Перевірка циклічних залежностей (категорія не може бути батьком самої себе)
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Category page now supports editing via Dialog: name, slug, description, parent (shadcn Select), sortOrder, active (Switch). Created Switch component. Both create/edit use shadcn Select instead of native. Circular parent refs prevented.
<!-- SECTION:FINAL_SUMMARY:END -->
