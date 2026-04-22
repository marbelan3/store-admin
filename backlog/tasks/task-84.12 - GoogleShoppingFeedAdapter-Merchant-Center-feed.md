---
id: TASK-84.12
title: GoogleShoppingFeedAdapter (Merchant Center feed)
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - google
dependencies: []
parent_task_id: TASK-84
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Feed-based. Host Google Shopping XML feed на public URL. Google Merchant Center pulls щоденно. Required: g:id, g:title, g:description, g:link, g:image_link, g:availability, g:price, g:brand, g:gtin, g:condition, g:google_product_category.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Feed endpoint /api/public/feeds/google/{tenantSlug}/products.xml signed
- [ ] #2 Google Product Category mapping (з нашої Category)
- [ ] #3 Feed оновлюється при зміні продукту/стоку
- [ ] #4 Optional: Content API integration для real-time (later)
- [ ] #5 Merchant Center setup docs у UI
<!-- AC:END -->
