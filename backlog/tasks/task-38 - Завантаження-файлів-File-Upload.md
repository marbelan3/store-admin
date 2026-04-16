---
id: TASK-38
title: Завантаження файлів (File Upload)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:34'
labels:
  - backend
  - frontend
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ProductMedia зберігає тільки URL — реального upload немає. Потрібен ендпоінт для завантаження зображень з валідацією типу/розміру. Зберігання: локальна FS для MVP з абстракцією для S3/MinIO.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 POST /api/media/upload — multipart upload з валідацією (тільки зображення, макс 5MB)
- [ ] #2 Створено FileStorageService з інтерфейсом та LocalFileStorageService реалізацією
- [ ] #3 Завантажені файли доступні через GET /api/media/{filename}
- [ ] #4 Генерація thumbnails (опціонально, або resize на фронті)
- [ ] #5 Інтеграція з формою товару — drag-and-drop upload замість вводу URL
- [ ] #6 Налаштування upload.dir в application.yml
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented file upload for product media images.

Changes:
- Created FileStorageService interface + LocalFileStorageService with tenant-isolated storage, content type validation (JPEG/PNG/GIF/WebP), 5MB limit, path traversal protection
- Created MediaController: POST /api/media/upload (auth required), GET /api/media/{tenantId}/{filename} (public, cached)
- Created MediaUploadResponse DTO
- Created frontend media.ts API client with uploadMedia() and getMediaUrl() helpers
- Enhanced ProductForm with drag-and-drop file upload, image preview thumbnails, upload progress
- Configured Spring multipart (5MB file / 10MB request) in application.yml
- Upload dir configurable via app.upload.dir (default ./uploads)
<!-- SECTION:FINAL_SUMMARY:END -->
