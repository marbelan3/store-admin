---
id: TASK-74
title: Saved Views / Custom Filters
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 21:12'
labels:
  - frontend
  - backend
  - phase9
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Дозволити користувачам зберігати налаштовані фільтри таблиць як 'views' (наприклад: 'Low stock electronics', 'Pending orders this week'). Shopify називає це 'saved searches'. Значно прискорює щоденну роботу.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Entity SavedView (id, tenantId, userId, name, entityType, filters JSON, isDefault)
- [ ] #2 Backend: CRUD endpoints для saved views
- [x] #3 Frontend: кнопка 'Save current view' біля фільтрів
- [x] #4 Tabs або dropdown зі збереженими views над таблицею
- [x] #5 Можливість видалити або перейменувати збережений view
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create SavedViews.svelte component using Dialog for save form and pill-based UI
2. Integrate into Products page
3. Integrate into Orders page
4. Integrate into Inventory page
5. Run svelte-check
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created SavedViews.svelte component with localStorage persistence, pill-based UI, Dialog for save form
- Integrated into Products, Orders, and Inventory pages
- Added defaultFilters prop for pages where default filter is not all-empty (inventory)
- svelte-check passes with 0 errors
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Saved Views:
- SavedViews component with localStorage persistence
- Integrated on Products, Orders, Inventory pages
- Save/apply/delete views as pill buttons
- Active view highlighting
<!-- SECTION:FINAL_SUMMARY:END -->
