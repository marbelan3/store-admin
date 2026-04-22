---
id: TASK-83.8
title: 'Extend Discount types — FREE_SHIPPING, BOGO, auto-apply'
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - promotions
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поточний DiscountType: PERCENTAGE, FIXED_AMOUNT. Додати: FREE_SHIPPING, BOGO (buy X get Y), TIERED (spend >A → B%), auto-apply boolean. Stacking rules (not combinable vs combinable).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 DiscountType enum розширено: FREE_SHIPPING, BOGO, TIERED
- [ ] #2 Discount.autoApply boolean; Discount.combinable boolean
- [ ] #3 BOGO: buyQuantity, getQuantity, getDiscount (100% = free)
- [ ] #4 TIERED: thresholds array [{minSubtotal, percent}]
- [ ] #5 CartCalculator враховує stacking rules
<!-- AC:END -->
