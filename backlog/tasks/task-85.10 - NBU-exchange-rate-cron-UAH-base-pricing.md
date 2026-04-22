---
id: TASK-85.10
title: NBU exchange rate cron + UAH-base pricing
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - international
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Daily cron pulls UAH/USD, UAH/EUR, UAH/PLN, UAH/GBP from NBU API (bank.gov.ua/NBUStatService). Tenant.baseCurrency = UAH. Products store price in UAH. On presentment conversion at NBU rate + markup%.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 NbuRateJob @Scheduled(cron = 0 0 9 * * *)
- [ ] #2 API: bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json
- [ ] #3 Store rates in currency_rates with provider='NBU'
- [ ] #4 Fallback if NBU down: cache previous
- [ ] #5 Markup% per tenant (default 2-5% volatility buffer)
- [ ] #6 Admin UI: /settings/currencies shows latest rates
<!-- AC:END -->
