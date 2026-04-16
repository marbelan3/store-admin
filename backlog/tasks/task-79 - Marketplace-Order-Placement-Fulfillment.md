---
id: TASK-79
title: 'Marketplace: Order Placement & Fulfillment'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:47'
updated_date: '2026-04-16 06:56'
labels:
  - backend
  - marketplace
dependencies:
  - TASK-78
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Розміщення замовлень на CJ Dropshipping при наявності marketplace товарів в order. Критичні пастки: pre-order price validation (ціна могла змінитись), stock recheck (stock не гарантується), order по CJ SKU (не productId), tracking з затримкою 24-48 годин.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Pre-order validation: перед розміщенням на CJ — перевірити актуальну ціну через CJ API, порівняти з source_price, alert якщо різниця > 5%
- [x] #2 Pre-order stock check: перевірити stock через CJ API, якщо 0 — спробувати fallback_cj_sku, якщо ні — notify seller
- [x] #3 Order placement: POST /api/marketplace/orders/{orderId}/place — створити order на CJ через createOrderV2, використовуючи cjSku з variant mapping
- [x] #4 Order split: якщо order містить OWN + MARKETPLACE товари — логічний split, marketplace частина йде на CJ
- [x] #5 OrderStatusPollJob (кожні 2 години): polling CJ order statuses, update local order status
- [x] #6 TrackingPollJob (кожні 4 години): polling tracking numbers з retry до 72 годин (tracking з'являється з затримкою)
- [x] #7 Error handling: CJ order failure (OOS, price changed) — retry з fallback SKU, або notify seller для manual resolution
- [x] #8 CJ order ID mapping: зберігати cjOrderId на order level для tracking
- [x] #9 Тести: pre-validation flow, order placement, status polling, tracking retry
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add cjOrderId + cjTrackingNumber fields to Order entity + migration 026
2. Create MarketplaceOrderService: pre-order validation, CJ order placement, order split logic
3. Create polling jobs: OrderStatusPollJob, TrackingPollJob (in MarketplaceSyncScheduler)
4. Create MarketplaceOrderController: POST /api/marketplace/orders/{orderId}/place
5. Integration tests
6. Compile & test
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Marketplace order placement on CJ Dropshipping with pre-validation, polling, and tracking.

Changes:
- Order entity: added cjOrderId + cjTrackingNumber fields
- Migration 026: adds CJ fields + index on orders table
- MarketplaceOrderService:
  - placeOrder: pre-order price validation (>5% diff warning), pre-order stock check with fallback SKU, CJ order creation via createOrderV2, order split (only MARKETPLACE items sent to CJ)
  - pollOrderStatuses: polls CJ for PROCESSING orders, updates to SHIPPED/DELIVERED/CANCELLED
  - pollTracking: retries tracking number retrieval for SHIPPED orders without tracking
- MarketplaceSyncScheduler: added orderStatusPollJob (every 2h) and trackingPollJob (every 4h)
- MarketplaceOrderController: POST /api/marketplace/orders/{orderId}/place
- DTOs: PlaceMarketplaceOrderRequest, MarketplaceOrderResultDto
- 5 integration tests: successful placement, duplicate prevention, wrong status rejection, poll jobs safety

Tests: all 196 pass
<!-- SECTION:FINAL_SUMMARY:END -->
