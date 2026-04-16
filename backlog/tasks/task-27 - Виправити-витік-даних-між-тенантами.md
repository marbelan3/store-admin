---
id: TASK-27
title: Виправити витік даних між тенантами
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:28'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
findAllById для categories/tags в ProductService не фільтрує по tenant_id. Зловмисник може прив'язати свій продукт до категорій іншого тенанта. Також bulkAction завантажує продукти без tenant filter.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 loadCategories/loadTags використовують findAllByTenantIdAndIdIn замість findAllById
- [ ] #2 bulkAction фільтрує продукти по tenantId на рівні БД, не в пам'яті
- [ ] #3 Додано методи findAllByTenantIdAndIdIn в CategoryRepository та TagRepository
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added findByTenantIdAndIdIn to CategoryRepository, TagRepository, ProductRepository. Updated loadCategories/loadTags and bulkAction to filter by tenant at DB level.
<!-- SECTION:FINAL_SUMMARY:END -->
