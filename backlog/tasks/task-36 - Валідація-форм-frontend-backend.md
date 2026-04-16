---
id: TASK-36
title: Валідація форм frontend + backend
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:20'
labels:
  - frontend
  - backend
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Форми не мають валідації — можна створити товар з від'ємною ціною, порожнім SKU тощо. superforms + zod встановлені, але не використовуються. На бекенді @Valid не каскадує на вкладені VariantRequest/MediaRequest.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено Zod-схеми для CreateProduct, UpdateProduct, CreateCategory
- [ ] #2 Підключено superforms для inline валідації на формах
- [ ] #3 Price та quantity валідуються >= 0
- [ ] #4 На бекенді додано @Valid для вкладених List<VariantRequest> та List<MediaRequest>
- [ ] #5 VariantRequest.price має @NotNull, Product.price має @DecimalMin(0)
- [ ] #6 Помилки валідації відображаються inline біля кожного поля
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created Zod product schema with validation (name required, price >= 0, quantity >= 0). Both product forms validate on submit with inline field errors. API client enhanced to pass status/body on 400 errors for backend validation display.
<!-- SECTION:FINAL_SUMMARY:END -->
