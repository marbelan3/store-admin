---
id: TASK-4
title: 'MySQL schema: core tables with Liquibase'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:30'
labels:
  - backend
  - database
  - phase-1
dependencies:
  - TASK-1
  - TASK-3
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create Liquibase changelogs for core tables:

1. tenants (id, name, slug, logo_url, settings JSON, is_active, created_at, updated_at)
2. users (id, tenant_id FK, email, name, avatar_url, oauth_provider, oauth_provider_id, role ENUM, is_active, created_at)
3. products (id, tenant_id FK, name, slug, description, short_description, price, compare_at_price, currency, status ENUM, sku, track_inventory, quantity, weight, attributes JSON, created_at, updated_at, deleted_at)
4. product_variants (id, tenant_id FK, product_id FK, name, sku, price, compare_at_price, quantity, options JSON, sort_order, is_active)
5. product_media (id, tenant_id FK, product_id FK, url, alt_text, media_type, sort_order, is_primary)
6. categories (id, tenant_id FK, parent_id FK self-ref, name, slug, path VARCHAR for materialized path, sort_order, is_active)
7. product_categories (product_id, category_id - M:N)
8. tags + product_tags

All tables with tenant_id have composite indexes (tenant_id, id) and (tenant_id, slug) where applicable.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All tables created via Liquibase migration
- [ ] #2 Indexes on tenant_id columns
- [ ] #3 Foreign keys with proper cascade
- [ ] #4 Soft delete (deleted_at) on products
<!-- AC:END -->
