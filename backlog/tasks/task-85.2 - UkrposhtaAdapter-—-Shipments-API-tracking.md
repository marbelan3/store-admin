---
id: TASK-85.2
title: UkrposhtaAdapter — Shipments API + tracking
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - shipping
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Ukrposhta Shipments API: shipment create, label, tracking. Tariffs: Standard, Express, EMS. Integration via production API (OAuth2).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UkrposhtaCredentials (clientId/secret) encrypted
- [ ] #2 Shipment create via Shipments API
- [ ] #3 Label generation (PDF)
- [ ] #4 Tracking events parsing
- [ ] #5 Tariff matrix: Standard/Express/EMS with weight+destination
- [ ] #6 Tests with fixtures
<!-- AC:END -->
