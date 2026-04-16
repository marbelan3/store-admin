---
id: TASK-57
title: Webhooks для зовнішніх інтеграцій
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - backend
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Механізм відправки HTTP-нотифікацій при зміні товарів/залишків, щоб зовнішні сервіси реагували в реальному часі замість polling Storefront API.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено entity WebhookEndpoint (URL, events, secret, active)
- [ ] #2 UI для налаштування webhook endpoints per tenant
- [ ] #3 Відправка POST з payload при: product.created, product.updated, product.deleted
- [ ] #4 Відправка при: inventory.updated, category.updated
- [ ] #5 HMAC підпис payload для верифікації
- [ ] #6 Retry з exponential backoff при невдалій доставці
<!-- AC:END -->
