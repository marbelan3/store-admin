---
id: TASK-66
title: Discounts & Promotions (Backend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:50'
labels:
  - backend
  - phase8
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Движок знижок та промокодів: відсоткові/фіксовані знижки, купони, автоматичні правила, діапазони дат. Прямий вплив на виручку тенантів — є у всіх конкурентів.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Entity Discount (id, tenantId, name, code, type: PERCENTAGE/FIXED_AMOUNT, value, minOrderAmount)
- [x] #2 Поля Discount: usageLimit, usageCount, startsAt, endsAt, isActive, appliesToAll)
- [x] #3 Entity DiscountProduct (discountId, productId) для знижок на конкретні товари
- [x] #4 Entity DiscountCategory (discountId, categoryId) для знижок на категорії
- [x] #5 Liquibase міграції для discounts, discount_products, discount_categories
- [x] #6 DiscountService: CRUD, validate code, apply to order, check eligibility
- [x] #7 REST endpoints: CRUD /api/discounts, POST /api/discounts/validate?code=...
- [x] #8 Автоматична деактивація при досягненні usageLimit або endsAt
- [x] #9 Tenant isolation та тести
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Discounts & Promotions backend:
- Entities: Discount, DiscountType, DiscountProduct, DiscountCategory
- Migration 023 for all tables with indexes and unique constraints
- DiscountService: CRUD, toggleActive, validateCode (checks expiry/usage/min order), incrementUsage
- REST: full CRUD + POST /validate?code=&orderAmount=
- 12 integration tests covering validation, expiry, usage limits, tenant isolation
- All tests passing
<!-- SECTION:FINAL_SUMMARY:END -->
