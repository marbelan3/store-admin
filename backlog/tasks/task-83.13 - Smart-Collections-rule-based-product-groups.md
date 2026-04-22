---
id: TASK-83.13
title: Smart Collections (rule-based product groups)
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - catalog
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Зараз тільки flat categories. Додати Collection (manual або rule-based: price > 500, tag = hot, inStock, category). Product × Collection — N:M (для manual). Compiler переводить rules у query. UI: builder.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Collection (type: MANUAL | RULE_BASED), CollectionProduct, CollectionRule
- [ ] #2 Rule compiler → Specification<Product>
- [ ] #3 API /api/collections CRUD + /products?collection=
- [ ] #4 UI: collection builder з rule chips
- [ ] #5 Storefront endpoint: GET /api/public/collections
<!-- AC:END -->
