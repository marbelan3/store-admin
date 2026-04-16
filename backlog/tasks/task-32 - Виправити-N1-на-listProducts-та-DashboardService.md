---
id: TASK-32
title: Виправити N+1 на listProducts та DashboardService
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - performance
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ProductMapper.toDto() звертається до 4 lazy колекцій (variants, media, categories, tags) — це 80+ запитів на 20 продуктів. DashboardService завантажує ВСІ записи для .size() замість COUNT.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано @EntityGraph або JOIN FETCH для ProductRepository.findByTenantId
- [ ] #2 Створено лайтовий ProductListDto без колекцій для лістингу
- [ ] #3 DashboardService використовує countByTenantId() замість .findAll().size()
- [ ] #4 Додано count методи в CategoryRepository, TagRepository, UserRepository
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created ProductListDto for lightweight list queries (1 query with subquery for primary image instead of 80+). Added @BatchSize(20) on Product collections. Dashboard uses countByTenantId() instead of findAll().size().
<!-- SECTION:FINAL_SUMMARY:END -->
