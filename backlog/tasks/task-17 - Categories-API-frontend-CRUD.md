---
id: TASK-17
title: 'Categories: API + frontend CRUD'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 12:01'
updated_date: '2026-04-15 12:55'
labels:
  - fullstack
  - phase-1
dependencies:
  - TASK-8
  - TASK-12
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Backend: Categories REST API
- GET /api/v1/categories - list (tree structure)
- POST /api/v1/categories - create
- PUT /api/v1/categories/{id} - update
- DELETE /api/v1/categories/{id} - delete (check no products assigned)

Frontend: Categories management page
- Tree view of categories
- Create/edit form (name, slug, parent, image)
- Drag & drop reordering
- Delete with confirmation
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Categories tree API works
- [ ] #2 Frontend tree view renders
- [ ] #3 CRUD operations work
- [ ] #4 Cannot delete category with products
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Full Categories CRUD: CategoryService, CategoryController (tree/flat list, create with parent, update, delete), MapStruct mapper, DTOs. Frontend: category tree view, create dialog with parent selector, delete.
<!-- SECTION:FINAL_SUMMARY:END -->
