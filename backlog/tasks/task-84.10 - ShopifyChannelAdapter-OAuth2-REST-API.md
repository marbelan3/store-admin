---
id: TASK-84.10
title: 'ShopifyChannelAdapter (OAuth2, REST API)'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - shopify
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Перша реальна outbound інтеграція. Shopify Admin API v2024-10. OAuth2 install flow. Products push через productCreate/productUpdate mutations (REST поки, GraphQL — опц). Inventory + price updates. Webhooks: orders/create, orders/updated.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 OAuth2 install flow: /api/channels/shopify/install → consent → callback
- [ ] #2 Store access_token в SalesChannelCredentials encrypted
- [ ] #3 Product push: title, description, images, variants, tags, collections
- [ ] #4 Inventory/price bulk updates через inventoryLevelSet
- [ ] #5 Webhook: orders/create, orders/updated — HMAC validated
- [ ] #6 Tests з recorded fixtures (no live API in CI)
<!-- AC:END -->
