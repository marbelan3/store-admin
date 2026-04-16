---
id: TASK-60
title: Media Manager (Backend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:13'
labels:
  - backend
  - phase5
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Централізований менеджер медіа-файлів для товарів. Завантаження зображень, зберігання метаданих, підтримка alt text та сортування. Конкуренти (Shopify, Saleor, Medusa) всі мають повноцінний media manager.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Створити entity Media (id, tenantId, filename, originalName, mimeType, size, url, altText, sortOrder)
- [x] #2 Створити entity ProductMedia (productId, mediaId, sortOrder) для зв'язку many-to-many
- [x] #3 Liquibase міграція для таблиць media та product_media
- [x] #4 MediaService: upload, delete, updateAltText, reorder
- [x] #5 REST endpoints: POST /api/media/upload, DELETE /api/media/{id}, PUT /api/media/{id}
- [x] #6 GET /api/products/{id}/media — список зображень товару з сортуванням
- [x] #7 POST /api/products/{id}/media — прив'язка медіа до товару
- [x] #8 Обмеження: max 10MB per file, тільки image/*, max 20 зображень на товар
- [x] #9 Tenant isolation: медіа належать тенанту
- [x] #10 Тести: upload, delete, tenant isolation
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Media Manager backend:
- Media entity with file metadata (filename, mimeType, size, url, altText)
- ProductMedia entity for many-to-many product↔media link
- Liquibase migration 020 for media + product_media tables
- MediaService: upload, delete, updateAltText, link/unlink to products, reorder
- MediaController: POST /api/media/upload, GET/PUT/DELETE /api/media/{id}
- ProductMediaController: GET/POST/DELETE /api/products/{id}/media, PUT reorder
- Constraints: max 10MB, image/* only, max 20 per product
- 12 integration tests covering upload, validation, tenant isolation, linking, reorder
- All 111 tests passing
<!-- SECTION:FINAL_SUMMARY:END -->
