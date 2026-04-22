---
id: TASK-85.6
title: EbayAdapter — Trading API + OAuth2
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
eBay Trading API (legacy, stable). OAuth2 with consent flow. AddItem, ReviseItem, EndItem, GetOrders. Categories via GetCategories (ID -> breadcrumb). Condition ID: New/Used.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 OAuth2 consent flow
- [ ] #2 EbayCredentials with access_token + refresh_token
- [ ] #3 OutboundChannelAdapter: publish -> AddItem; update -> ReviseItem; delist -> EndItem
- [ ] #4 GetOrders every 10min (polling)
- [ ] #5 Categories sync daily
- [ ] #6 Item condition mapping
- [ ] #7 Tests with fixtures
<!-- AC:END -->
