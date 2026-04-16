---
id: TASK-39
title: Пошук товарів по назві/SKU/опису
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:02'
labels:
  - backend
  - frontend
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Зараз є тільки фільтр по статусу. Потрібен повнотекстовий пошук для адмінів з сотнями товарів. JPA Specification для фільтрації по категорії, тегу, діапазону цін, наявності.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Додано текстове поле пошуку над таблицею товарів з debounce 300ms
- [x] #2 Бекенд підтримує параметр search для пошуку по name, sku, description
- [x] #3 Фільтрація по категорії через select
- [x] #4 Фільтрація по діапазону цін (від-до)
- [x] #5 Створено ProductSpecification для комбінування фільтрів
- [x] #6 Пошуковий запит зберігається в URL params
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create ProductSpecification.java using JPA Criteria API
2. Update ProductRepository to extend JpaSpecificationExecutor
3. Update ProductService.getProducts() to accept filter params and use Specification
4. Update ProductController to accept new query params
5. Run compileJava to verify compilation
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Created ProductSpecification.java with combinable JPA Criteria filters. Updated ProductRepository, ProductService, and ProductController. Backward compatible. compileJava passes.

Frontend side done:
- Search box with 300ms debounce added to Products page
- Category filter dropdown added
- URL params synced (search, categoryId)
- Backend search/filter params plumbed through API client
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added product search and filtering with JPA Specification.

Backend: ProductSpecification combining tenantId, search (LIKE on name/sku/description), status, categoryId (ManyToMany join), priceMin/priceMax. ProductRepository extended with JpaSpecificationExecutor. Backward-compatible API.
Frontend: Search input with 300ms debounce, category dropdown filter, all params synced to URL.
<!-- SECTION:FINAL_SUMMARY:END -->
