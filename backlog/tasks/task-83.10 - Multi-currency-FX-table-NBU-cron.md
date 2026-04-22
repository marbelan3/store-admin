---
id: TASK-83.10
title: Multi-currency + FX table + NBU cron
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - international
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поки один currency на Tenant. Додати: CurrencyRate (base → target, rate, provider, fetchedAt), Price.amountMoney {amount, currency}. NBU cron pulls UAH/USD, UAH/EUR, UAH/PLN щодня. Presentment: auto-convert на storefront за user currency.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entity CurrencyRate, scheduled NbuRateJob (daily)
- [ ] #2 Prices stored в base currency, convertedAmount обчислюється
- [ ] #3 Tenant.supportedCurrencies: []
- [ ] #4 Storefront API повертає пару base + presentment
- [ ] #5 Admin UI: /settings/currencies
<!-- AC:END -->
