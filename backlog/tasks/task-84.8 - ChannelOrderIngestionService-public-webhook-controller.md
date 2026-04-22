---
id: TASK-84.8
title: ChannelOrderIngestionService + public webhook controller
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - webhooks
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Ingest incoming orders з зовнішніх каналів. Webhook endpoint /api/public/channels/{channelId}/webhook (skip JWT, validate HMAC). Idempotency via (channel_id, external_order_id). Creates local Order з source_channel_id.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase 033-create-channel-orders.yaml + 037-add-channel-columns-to-orders.yaml
- [ ] #2 ChannelWebhookController з HMAC signature validation (per-channel)
- [ ] #3 ChannelOrderIngestionService.ingest(channel, payload) idempotent
- [ ] #4 OrderService.createOrderFromChannel() — новий overload suppresses outbound stock decrement on source channel
- [ ] #5 Notification CHANNEL_ORDER_RECEIVED
<!-- AC:END -->
