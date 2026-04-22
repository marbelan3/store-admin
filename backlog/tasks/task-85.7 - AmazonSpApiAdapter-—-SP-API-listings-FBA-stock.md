---
id: TASK-85.7
title: AmazonSpApiAdapter — SP-API listings + FBA stock
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - channels
  - international
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Amazon Selling Partner API (modern). OAuth + AWS IAM signing. Listings API for publish, Orders API for ingestion, Feeds API for bulk. FBA support optional. Rate limits strict - respect tokens bucket.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 AWS Signature v4 signing
- [ ] #2 Amazon SP-API OAuth2 install flow
- [ ] #3 Listings API: putListingsItem, patchListingsItem, deleteListingsItem
- [ ] #4 Orders API: getOrders + getOrderItems (poll every 5min)
- [ ] #5 Feeds API for bulk updates
- [ ] #6 Rate limiter (token bucket) per endpoint
- [ ] #7 FBA stock sync optional
- [ ] #8 Tests with fixtures
<!-- AC:END -->
