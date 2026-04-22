---
id: TASK-82.11
title: Empty states overhaul — illustrations + secondary actions
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux
dependencies: []
parent_task_id: TASK-82
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити generic grey-circle EmptyState на per-feature illustrations (products, orders, inventory, channels, customers, marketplace) + primary CTA + "Learn more" link. Для first-time users показувати квіклінки до docs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено SVG illustrations для 6 основних features
- [ ] #2 EmptyState приймає props: icon, illustration, title, description, primaryAction, secondaryLink
- [ ] #3 Продукти/orders/customers/inventory/channels/marketplace використовують новий патерн
- [ ] #4 Dark mode варіанти illustrations
<!-- AC:END -->
