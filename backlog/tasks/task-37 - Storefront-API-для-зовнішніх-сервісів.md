---
id: TASK-37
title: Storefront API для зовнішніх сервісів
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:34'
labels:
  - backend
  - api
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Read-only API під /api/v1/storefront/ з авторизацією через API-ключ (service-to-service). Це основна причина існування адмінки — дані мають бути доступні для вітрини та інших сервісів. API-ключ прив'язаний до тенанта.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /api/v1/storefront/products — пагінований список (тільки ACTIVE), фільтри по category/tag/search/sort
- [ ] #2 GET /api/v1/storefront/products/{slug} — повний товар з варіантами, медіа, категоріями
- [ ] #3 GET /api/v1/storefront/categories — дерево активних категорій
- [ ] #4 GET /api/v1/storefront/categories/{slug}/products — товари в категорії
- [ ] #5 GET /api/v1/storefront/inventory/{sku} та /inventory/bulk — перевірка наявності
- [ ] #6 Авторизація через X-API-Key header, прив'язаний до тенанта
- [ ] #7 Створено таблицю api_keys з Liquibase міграцією
- [ ] #8 costPrice варіантів НЕ включається у відповідь storefront API
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Storefront API for external service integration.

Changes:
- Created StorefrontController with endpoints: GET /products, /products/{slug}, /categories, /categories/{slug}, /categories/{slug}/products, /inventory/{sku}, /inventory/bulk
- Created StorefrontService with product/category/inventory queries, filtered to ACTIVE products only
- Created DTOs: StorefrontProductDto, StorefrontProductDetailDto, StorefrontCategoryDto, StorefrontInventoryDto (no costPrice exposed)
- Created ApiKey entity + repository + migration 012-create-api-keys.yaml
- Created ApiKeyAuthenticationFilter (X-API-Key header auth for /api/v1/storefront/** paths)
- Created ApiKeyController for admin CRUD of API keys (generate, list, toggle, delete)
- Added storefront security filter chain (Order 2) in SecurityConfig
- Supports sorting (price_asc/desc, name, newest), pagination, category/tag/search filtering
<!-- SECTION:FINAL_SUMMARY:END -->
