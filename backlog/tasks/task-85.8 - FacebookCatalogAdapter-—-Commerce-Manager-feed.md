---
id: TASK-85.8
title: FacebookCatalogAdapter — Commerce Manager feed
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - channels
  - social
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Facebook/Instagram Shops. Commerce Manager consumes catalog feed (CSV/XML) or real-time Catalog API. Support both. Required: id, title, description, availability, condition, price, link, image_link, brand, google_product_category.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Feed endpoint /api/public/feeds/facebook/{tenantSlug}/products.csv signed
- [ ] #2 Catalog API integration (Graph API) as alternative
- [ ] #3 Required fields: id, title, description, availability, condition, price, link, image_link, brand
- [ ] #4 Optional: sale_price, sale_price_effective_date, item_group_id for variants
- [ ] #5 Insta Shopping tags optional (product tagging in posts)
- [ ] #6 Tests with fixtures
<!-- AC:END -->
