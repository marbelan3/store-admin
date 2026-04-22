---
id: TASK-83.19
title: Product content i18n (complete TASK-56)
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - i18n
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-56 — єдиний To Do task. Додати ProductTranslation (product × language: name, description, seo_title, seo_description, slug). Admin UI: tabs UA/RU/EN у product form. Storefront бере ?lang чи Accept-Language.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entity ProductTranslation + CategoryTranslation
- [ ] #2 Admin UI: language tabs на product/category edit
- [ ] #3 Storefront API: GET /api/public/products?lang=uk повертає translated content
- [ ] #4 Default language на Tenant
- [ ] #5 Fallback: якщо translation відсутня, показати default
<!-- AC:END -->
