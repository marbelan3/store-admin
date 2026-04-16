---
id: TASK-61
title: Media Manager (Frontend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:20'
labels:
  - frontend
  - phase5
dependencies:
  - TASK-60
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
UI для завантаження та управління зображеннями товарів. Drag-and-drop upload, reorder, alt text, preview. Ключова фіча всіх сучасних e-commerce адмінок.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Компонент MediaUploader: drag-and-drop зона + кнопка вибору файлів
- [x] #2 Preview завантажених зображень з progress bar
- [x] #3 Drag-and-drop reorder зображень товару
- [x] #4 Inline editing alt text для кожного зображення
- [x] #5 Кнопка видалення з confirm dialog
- [x] #6 Інтеграція на сторінку створення/редагування товару
- [x] #7 Підтримка multiple upload (до 20 файлів)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Rewrite media.ts API client with full CRUD + product media endpoints
2. Create MediaUploader.svelte component with drag-drop, grid, reorder, alt text editing, delete
3. Integrate MediaUploader into ProductForm.svelte replacing existing basic media section
4. Add product thumbnail to products list page
5. Run svelte-check
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Rewrote media.ts API client with all endpoints: upload, delete, updateAltText, getProductMedia, linkMediaToProduct, unlinkMediaFromProduct, reorderProductMedia
- Created MediaUploader.svelte with drag-and-drop zone, preview thumbnails during upload, image grid with reorder (up/down arrows), inline alt text editing (click to edit, save on blur/Enter), delete with confirm dialog, primary badge
- Integrated into ProductForm: edit mode uses MediaUploader with product media API, create mode retains existing basic upload
- Added product thumbnail column to products list table
- svelte-check: 0 errors, 0 warnings
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Media Manager frontend:
- API client (media.ts) for upload, delete, altText, link/unlink, reorder
- MediaUploader component: drag-and-drop zone, instant preview, inline alt text editing, up/down reorder, delete with confirm, Primary badge
- Validation: image/* only, max 10MB, max 20 per product
- Integrated in ProductForm (edit mode uses full MediaUploader)
- Product thumbnails in products list table
- svelte-check: 0 errors, 0 warnings
<!-- SECTION:FINAL_SUMMARY:END -->
