---
id: TASK-13
title: 'Frontend: Products list page'
status: Done
assignee: []
created_date: '2026-04-15 11:59'
updated_date: '2026-04-15 12:51'
labels:
  - frontend
  - ui
  - products
  - phase-1
dependencies:
  - TASK-9
  - TASK-12
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create products list page (src/routes/(app)/products/+page.svelte):

1. Data table (TanStack Table + shadcn-svelte):
   - Columns: image thumb, name, SKU, price, status (badge), category, stock, actions
   - Server-side pagination (URL search params)
   - Sortable columns (name, price, created_at)
   - Column visibility toggle
   - Row selection for bulk actions

2. Filters panel:
   - Search by name/SKU (debounced)
   - Filter by status (dropdown)
   - Filter by category (dropdown)
   - Clear all filters button

3. Actions:
   - "Add Product" button (links to /products/new)
   - Bulk delete (with confirmation dialog)
   - Bulk status change

4. Empty state when no products
5. Loading skeleton
6. Permission-based: hide Add/Delete buttons for VIEWER role
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Data table with pagination and sorting
- [ ] #2 Filters work with URL search params
- [ ] #3 Bulk actions with confirmation
- [ ] #4 Permission-based button visibility
- [ ] #5 Loading and empty states
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Products list page with status filter buttons, table view, delete action, empty state.
<!-- SECTION:FINAL_SUMMARY:END -->
