---
id: TASK-82.13
title: Inline filter builder (chip-based) for list pages
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux-pattern
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замість розкиданих dropdowns — єдиний filter bar з + Add Filter → picker → chip. Кожен chip: field operator value. Remove chip через X. Застосовується на products, orders, customers, marketplace products.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 <FilterBuilder> компонент з chip-based UI
- [ ] #2 Filter picker: field (status, price, category, date) + operator (=, >, <, in) + value
- [ ] #3 Chips можна видаляти/редагувати/дублювати
- [ ] #4 Filters серіалізуються в URL query params
- [ ] #5 Integrated з SavedViews
<!-- AC:END -->
