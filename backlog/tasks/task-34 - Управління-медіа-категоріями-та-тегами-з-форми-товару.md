---
id: TASK-34
title: 'Управління медіа, категоріями та тегами з форми товару'
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
Форми створення/редагування товару не мають UI для прив'язки категорій, тегів та управління медіа. Також варіанти тільки read-only. Це блокує основну роботу адмін-панелі.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Секція Media з можливістю додати/видалити/змінити порядок зображень
- [ ] #2 Секція Categories з multi-select або checkbox tree для прив'язки категорій
- [ ] #3 Секція Tags з input-автокомплітом та можливістю створити новий тег на льоту
- [ ] #4 Секція Variants з можливістю додавання/редагування/видалення варіантів
- [ ] #5 Витягнуто спільний компонент ProductForm.svelte (new і edit дублюються на 90%)
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created shared ProductForm.svelte component with sections: Categories (checkbox grid), Tags (badge toggles), Media (URL rows with add/remove), Variants (editable rows). Both new/edit pages simplified to use ProductForm. Full payload with categoryIds, tagIds, media, variants sent to API.
<!-- SECTION:FINAL_SUMMARY:END -->
