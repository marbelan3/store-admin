---
id: TASK-69
title: Inline Editing в таблицях
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 21:03'
labels:
  - frontend
  - phase9
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Редагування ціни, залишків та статусу прямо в таблиці без переходу на окрему сторінку. BigCommerce і Saleor активно використовують. Економить масу часу при щоденних операціях.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Inline editing ціни в таблиці товарів (click to edit, Enter to save, Escape to cancel)
- [x] #2 Inline editing залишків в таблиці inventory
- [x] #3 Inline editing статусу товару через dropdown в таблиці
- [x] #4 Visual feedback: highlight edited cell, show save/cancel icons
- [x] #5 Auto-save з debounce або explicit save button
- [x] #6 Optimistic update з rollback при помилці
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create reusable InlineEditCell.svelte component with Svelte 5 runes
2. Create InlineStatusSelect.svelte for status cycling in products table
3. Add updateProduct inline-edit API helper (patchProduct for partial updates)
4. Add updateInventoryItem API helper for single-item threshold updates
5. Integrate InlineEditCell into products table (price column)
6. Integrate InlineStatusSelect into products table (status column)
7. Enhance inventory table with InlineEditCell for threshold column
8. Run svelte-check to validate
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created InlineEditCell.svelte component with full keyboard support (Enter/Escape/Tab), auto-focus+select, optimistic updates with rollback, visual feedback (pencil icon on hover, blue ring while editing, green flash on success, spinner while saving)
- Created InlineStatusSelect.svelte component with dropdown for cycling product statuses, same visual feedback pattern
- Added patchProduct() to products API for partial updates
- Added updateInventoryItem() to inventory API for single-item updates
- Integrated InlineEditCell for Price column in products table (currency type with $ prefix display)
- Integrated InlineStatusSelect for Status column in products table
- Integrated InlineEditCell for Quantity column in inventory table (replacing plain Input)
- All inline editing gated behind auth.canEdit — viewers see plain text
- svelte-check passes with 0 errors
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Inline Editing:
- InlineEditCell component: click-to-edit, auto-focus, Enter/blur save, Escape cancel, spinner, success flash
- InlineStatusSelect component: inline dropdown for status changes
- Products table: inline price editing + status select
- Inventory table: inline quantity editing
- Optimistic updates with rollback on error
- svelte-check: 0 errors
<!-- SECTION:FINAL_SUMMARY:END -->
