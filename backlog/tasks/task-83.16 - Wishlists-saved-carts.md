---
id: TASK-83.16
title: Wishlists / saved carts
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - customers
dependencies: []
parent_task_id: TASK-83
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Для customers: Wishlist (n per customer), WishlistItem (variant). SavedCart (in-progress cart persisted). Storefront API. Customer timeline показує: added to wishlist, saved cart.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Wishlist, WishlistItem, SavedCart, SavedCartLine
- [ ] #2 API: /api/customers/{id}/wishlists, /saved-carts
- [ ] #3 Storefront: authenticated customer може додавати/видаляти
- [ ] #4 Notifications на back-in-stock для wishlist items
<!-- AC:END -->
