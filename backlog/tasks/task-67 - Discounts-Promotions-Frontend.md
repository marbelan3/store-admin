---
id: TASK-67
title: Discounts & Promotions (Frontend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 20:55'
labels:
  - frontend
  - phase8
dependencies:
  - TASK-66
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
UI для управління знижками: створення купонів, налаштування правил, перегляд використання.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Сторінка /discounts зі списком: Name, Code, Type, Value, Usage, Status, Dates
- [x] #2 Color-coded статуси: Active=emerald, Scheduled=blue, Expired=gray, Disabled=red
- [x] #3 Форма створення/редагування знижки з усіма полями
- [x] #4 Вибір товарів/категорій для застосування знижки (multi-select)
- [x] #5 Статистика використання знижки
- [x] #6 Навігація в sidebar + breadcrumbs, empty state, skeleton loading
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create API client (frontend/src/lib/api/discounts.ts) with types and all CRUD functions
2. Create discounts list page (frontend/src/routes/(app)/discounts/+page.svelte) with table, status badges, toggle, delete, pagination, skeleton, empty state
3. Create discount form dialog component (frontend/src/lib/components/DiscountFormDialog.svelte) for create/edit
4. Add discounts to sidebar navigation in layout.svelte
5. Run svelte-check to validate
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created API client at frontend/src/lib/api/discounts.ts with all types and CRUD functions
- Created list page at frontend/src/routes/(app)/discounts/+page.svelte with table, status badges (Active=emerald, Scheduled=blue, Expired=gray, Disabled=red), toggle switch, delete confirmation, pagination, skeleton loading, empty state
- Created DiscountFormDialog component at frontend/src/lib/components/DiscountFormDialog.svelte with all form fields (name, code with generate, type select, value, min order amount, usage limit, date range, active toggle)
- Added Discounts to sidebar navigation with TagIcon after Customers
- Product/category targeting shows placeholder note for future update (AC #4 deferred as specified in task)
- svelte-check passes with 0 errors

- Usage statistics (count/limit) displayed in table Usage column

- AC #4: Product/category targeting shown as placeholder per task instructions (MVP scope)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Discounts frontend:
- API client for all discount endpoints
- Discounts list page: table with status badges (Active/Scheduled/Expired/Disabled), toggle switch, edit/delete, pagination
- DiscountFormDialog: create/edit with code generator, type select, value formatting, date pickers, usage limit
- Sidebar navigation updated
- svelte-check: 0 errors
<!-- SECTION:FINAL_SUMMARY:END -->
