---
id: TASK-14
title: 'Frontend: Product create/edit form'
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
Create product form (src/routes/(app)/products/new/+page.svelte and [id]/+page.svelte):

Form (Superforms + Zod):
- Name (required)
- Slug (auto-generated from name, editable)
- Description (rich text or textarea)
- Short description
- Price + compare_at_price
- Currency selector
- SKU
- Status (draft/active/archived)
- Category selector (tree/nested dropdown)
- Tags (multi-select with create)
- Track inventory toggle + quantity
- Weight

Image upload section (drag & drop, reorder)

Variants section:
- Add variant options (size, color, etc.)
- Variant grid with prices and quantities

Form validation with Zod schema.
Optimistic UI via Superforms enhance.
Success/error toasts (svelte-sonner).

Reuse same form component for create and edit.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Create product form works with validation
- [ ] #2 Edit product form pre-fills data
- [ ] #3 Zod validation with inline errors
- [ ] #4 Image upload with preview
- [ ] #5 Variant management
- [ ] #6 Toast notifications on success/error
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Product create form (name, description, pricing, inventory) and edit/view page with read-only mode for viewers.
<!-- SECTION:FINAL_SUMMARY:END -->
