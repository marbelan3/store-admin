---
id: TASK-84.2
title: SalesChannel + Credentials domain — migrations 028-029
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Нова сутність SalesChannel (type, name, status, syncEnabled, defaultCurrency, defaultLocale, inventoryBufferPct, pricePolicy, lastSyncedAt). SalesChannelCredentials з encrypted storage. Entities, repositories, service, controller skeleton.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase 028-create-sales-channels.yaml
- [ ] #2 Liquibase 029-create-sales-channel-credentials.yaml
- [ ] #3 Entities SalesChannel, SalesChannelCredentials з TenantAwareEntity
- [ ] #4 Credentials encryption-at-rest через Jasypt
- [ ] #5 SalesChannelService CRUD + test() method
- [ ] #6 Controller: POST/GET/PUT/DELETE /api/channels
- [ ] #7 Unique (tenant_id, type, name)
<!-- AC:END -->
