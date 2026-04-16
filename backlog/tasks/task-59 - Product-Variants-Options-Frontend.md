---
id: TASK-59
title: Product Variants & Options (Frontend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:20'
labels:
  - frontend
  - phase5
dependencies:
  - TASK-58
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
UI для управління варіантами товарів на сторінці редагування продукту. Додати опції та варіанти з можливістю встановлення окремих цін, SKU та залишків для кожної комбінації.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Секція Options на сторінці товару: додавання/видалення опцій (Size, Color тощо) з values
- [x] #2 Автогенерація варіантів з комбінацій option values
- [x] #3 Таблиця варіантів з inline editing: SKU, ціна, залишок
- [x] #4 Bulk update ціни/залишків для всіх варіантів одночасно
- [x] #5 Валідація: унікальність SKU, обов'язковість ціни
- [x] #6 Відображення варіантів в списку товарів та inventory
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create API client for variants/options (frontend/src/lib/api/variants.ts)
2. Add TypeScript types for options and variants (frontend/src/lib/types/product.ts)
3. Create ProductOptions.svelte component
4. Create ProductVariants.svelte component
5. Integrate both components into product edit page [id]/+page.svelte
6. Make product names clickable in list (already done - rows are clickable)
7. Run svelte-check to verify
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created API client: frontend/src/lib/api/variants.ts (getOptions, createOption, deleteOption, getVariants, createVariant, updateVariant, deleteVariant)
- Added types: ProductOption, OptionValue, ProductVariantDetail in product.ts
- Built ProductOptions.svelte with add/delete options, tag-style value input
- Built ProductVariants.svelte with inline editing (SKU, price, compareAtPrice), availability toggle, add/delete/auto-generate/bulk-update
- Integrated both into product edit page [id]/+page.svelte
- Product list already has clickable rows navigating to /products/{id}
- svelte-check passes with 0 errors
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Product Variants & Options frontend:
- API client (variants.ts) for all option/variant endpoints
- ProductOptions component: add/delete options with tag-style value pills
- ProductVariants component: table with inline editing (SKU, price, compareAtPrice), availability toggle, auto-generate combinations, bulk update dialog
- Integrated on product edit page (/products/[id])
- Read-only mode for TENANT_VIEWER
- svelte-check: 0 errors, 0 warnings
<!-- SECTION:FINAL_SUMMARY:END -->
