---
id: TASK-84.3
title: Publication + ChannelListing domain — migrations 030-031
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
Publication (Product ↔ Channel M:N, status DRAFT/QUEUED/PUBLISHED/DELISTED/ERROR, excluded, priceOverride, qtyOverride, categoryMappingJson). ChannelListing (Variant × Publication: externalListingId, externalSku, publishedPrice, publishedQty, lastPushedAt, lastPushHash, listingStatus).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase 030-create-publications.yaml
- [ ] #2 Liquibase 031-create-channel-listings.yaml
- [ ] #3 Entities Publication, ChannelListing з tenantId + FK
- [ ] #4 PublicationService + ChannelListingService CRUD
- [ ] #5 Hash column (sha256 payload) для idempotency
- [ ] #6 Unique (tenant_id, channel_id, product_id)
<!-- AC:END -->
