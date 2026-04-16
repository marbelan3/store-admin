---
id: TASK-5
title: JPA Entities + Repositories
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:51'
labels:
  - backend
  - domain
  - phase-1
dependencies:
  - TASK-4
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create JPA entities for all core tables:

Product, ProductVariant, ProductMedia, Category, Tag, Tenant, User.

Each entity:
- @Entity with proper table mapping
- Hibernate @Filter(name="tenantFilter", condition="tenant_id = :tenantId") on tenant-scoped entities
- Auditable fields (created_at, updated_at) via @MappedSuperclass
- Soft delete via @SQLDelete and @Where on Product

Spring Data JPA repositories with custom query methods where needed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All entities map to DB schema
- [ ] #2 Hibernate tenant filter on all tenant-scoped entities
- [ ] #3 Soft delete works for products
- [ ] #4 Repositories with pagination support
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created all JPA entities (Product with soft delete, ProductVariant, ProductMedia, Category with hierarchy, Tag) extending TenantAwareEntity with Hibernate @Filter. Added repositories with pagination, filtering, and tenant-scoped queries.
<!-- SECTION:FINAL_SUMMARY:END -->
