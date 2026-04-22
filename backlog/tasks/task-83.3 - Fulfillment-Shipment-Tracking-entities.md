---
id: TASK-83.3
title: Fulfillment / Shipment / Tracking entities
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - fulfillment
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Сьогодні Order має statuses, shippingAmount та shippingAddress string — немає Fulfillment model. Додати: Fulfillment (N per order), FulfillmentLine, Shipment (carrier, tracking, label URL), TrackingEvent. Partial fulfillment: один order → кілька shipments.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Fulfillment, FulfillmentLine, Shipment, TrackingEvent
- [ ] #2 Order detail показує fulfillments + shipments + tracking timeline
- [ ] #3 API: POST /api/orders/{id}/fulfillments
- [ ] #4 Status derivation: Order.fulfillmentStatus розраховується з fulfillments
- [ ] #5 Nova Poshta + manual carriers підтримані
<!-- AC:END -->
