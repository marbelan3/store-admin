---
id: TASK-83.12
title: Metafields (extensible custom attributes)
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - catalog
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Product/Order/Customer/Category мають рідний attributes JSON. Додати структуровані Metafields: MetafieldDefinition (namespace, key, type, validations) + Metafield (value per instance). Типи: string, number, bool, date, reference, json, list.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: MetafieldDefinition, Metafield з ownerType + ownerId
- [ ] #2 Types: string, number, bool, date, reference, json, list
- [ ] #3 API: CRUD для definitions + inline read/write
- [ ] #4 UI: Custom Data editor на product/order/customer detail
- [ ] #5 Validation: regex, min/max, required
<!-- AC:END -->
