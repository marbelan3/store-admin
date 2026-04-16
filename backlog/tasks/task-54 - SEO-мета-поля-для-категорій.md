---
id: TASK-54
title: SEO мета-поля для категорій
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - backend
  - frontend
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Product вже має metaTitle/metaDescription, але Category — ні. Ці поля потрібні для Storefront API, щоб вітрина мала SEO-контент.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано metaTitle та metaDescription в Category entity
- [ ] #2 Liquibase міграція для нових колонок
- [ ] #3 CategoryDto та UpdateCategoryRequest включають SEO поля
- [ ] #4 Форма редагування категорії має секцію SEO
<!-- AC:END -->
