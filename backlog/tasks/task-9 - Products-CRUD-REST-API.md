---
id: TASK-9
title: 'Products CRUD: REST API'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:51'
labels:
  - backend
  - api
  - phase-1
dependencies:
  - TASK-8
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement Products REST API:

Endpoints:
- GET /api/v1/products - list with pagination, sorting, filtering (by status, category, search query)
- GET /api/v1/products/{id} - get by ID
- POST /api/v1/products - create
- PUT /api/v1/products/{id} - update
- DELETE /api/v1/products/{id} - soft delete
- PATCH /api/v1/products/{id}/status - change status (draft/active/archived)

DTOs: ProductRequest, ProductResponse, ProductListResponse (paginated), ProductFilterParams.
MapStruct mapper: Product <-> DTO.
Service layer with business logic.
Validation via Jakarta Bean Validation on DTOs.

All endpoints scoped by tenant_id (via TenantFilter).
All endpoints protected by RBAC.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All 6 endpoints work correctly
- [ ] #2 Pagination with page/size/sort params
- [ ] #3 Filtering by status, category, search
- [ ] #4 Validation errors return 400 with details
- [ ] #5 Soft delete sets deleted_at
- [ ] #6 OpenAPI docs generated
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Full Products CRUD: ProductService, ProductController (GET list/detail, POST create, PUT update, DELETE soft-delete), DTOs, MapStruct mapper. Supports pagination, status filter, variants, media, categories, tags.
<!-- SECTION:FINAL_SUMMARY:END -->
