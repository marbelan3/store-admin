---
id: TASK-85.1
title: 'NovaPoshtaAdapter — TTN, labels, tracking, parcel lockers'
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - shipping
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Nova Poshta API: createInternetDocument (TTN generation), getDocument (tracking), printMarkings (label PDF). Support address delivery, warehouse, parcel lockers. Cache cities/warehouses daily.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entity NovaPoshtaCredentials (apiKey encrypted)
- [ ] #2 CarrierAdapter interface (create, track, cancel, getLabel, getCities, getWarehouses)
- [ ] #3 createInternetDocument generates TTN with recipient, sender, cargo
- [ ] #4 Label PDF download by TTN
- [ ] #5 Webhook for tracking events (or polling every 2h)
- [ ] #6 Parcel lockers support (Warehouse.type = POSTOMAT)
- [ ] #7 Cities/warehouses cached in DB, refresh daily
- [ ] #8 Test coverage with recorded fixtures
<!-- AC:END -->
