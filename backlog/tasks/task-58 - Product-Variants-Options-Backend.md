---
id: TASK-58
title: Product Variants & Options (Backend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:13'
labels:
  - backend
  - phase5
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Додати підтримку варіантів товарів (розмір, колір, матеріал) з окремими цінами, SKU та залишками на складі. Без варіантів продавці змушені дублювати товари для кожної комбінації опцій.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Створити entity ProductOption (id, productId, name — e.g. Size, Color)
- [x] #2 Створити entity ProductOptionValue (id, optionId, value — e.g. S, M, L)
- [x] #3 Створити entity ProductVariant (id, productId, sku, price, compareAtPrice, weight, barcode)
- [x] #4 Створити join table product_variant_option_values для зв'язку варіант↔значення опцій
- [x] #5 Liquibase міграції для всіх нових таблиць з FK та індексами
- [x] #6 ProductVariantRepository з методами findByProductIdAndTenantId, existsBySkuAndTenantId
- [x] #7 ProductVariantService: CRUD варіантів, автогенерація SKU, валідація унікальності
- [x] #8 REST endpoints: GET/POST/PUT/DELETE /api/products/{id}/variants
- [x] #9 Inventory інтеграція: залишки трекаються per-variant, а не per-product
- [x] #10 Тести: unit + integration для варіантів, tenant isolation
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Product Variants & Options backend:
- Entities: ProductOption, ProductOptionValue with cascade relationships
- ProductVariant entity with ManyToMany to option values
- Liquibase migration 019 for all tables with FK and indexes
- ProductOptionRepository + ProductVariantRepository
- ProductOptionService: full CRUD for options, variants, SKU validation
- REST endpoints: GET/POST/DELETE /api/products/{id}/options, GET/POST/PUT/DELETE /api/products/{id}/variants
- MapStruct mapper for DTOs
- 10 integration tests covering CRUD, tenant isolation, SKU uniqueness
- All 111 tests passing
<!-- SECTION:FINAL_SUMMARY:END -->
