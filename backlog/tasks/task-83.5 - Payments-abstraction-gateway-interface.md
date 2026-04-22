---
id: TASK-83.5
title: Payments abstraction + gateway interface
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - payments
dependencies: []
parent_task_id: TASK-83
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поки немає Payment entity і gateway integration. Додати: PaymentIntent/Charge/Payout model + PaymentGatewayAdapter interface (authorize, capture, refund, void). Stub implementations: ManualPayment, Stripe, LiqPay. Інтегрувати з Order.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: PaymentIntent, Charge, Payout, Dispute
- [ ] #2 Interface PaymentGatewayAdapter з authorize/capture/refund/void
- [ ] #3 Adapters: ManualPayment (record-only), StripeAdapter (stub), LiqPayAdapter (stub)
- [ ] #4 Payment state machine: PENDING → AUTHORIZED → CAPTURED → REFUNDED
- [ ] #5 Webhook controller для gateway callbacks
<!-- AC:END -->
