---
id: TASK-42
title: Імпорт/Експорт товарів CSV
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:02'
labels:
  - backend
  - frontend
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Масовий імпорт товарів через CSV для швидкого наповнення каталогу. Експорт поточного каталогу для аналітики та бухгалтерії. Валідація та preview перед імпортом.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 POST /api/products/import — завантаження CSV файлу з товарами
- [x] #2 GET /api/products/export — експорт каталогу в CSV
- [x] #3 Валідація CSV перед імпортом з показом помилок по рядках
- [x] #4 Preview імпорту — показати що буде створено/оновлено перед підтвердженням
- [x] #5 UI на сторінці Products — кнопки Import та Export
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented CSV Import/Export.

Backend: ProductCsvController with GET /api/products/export (CSV download) and POST /api/products/import (validate, preview errors, create/update by SKU match). ProductCsvService with hand-rolled CSV parser (handles quoting, escaped quotes, max 1000 rows).
Frontend: Import CSV button (file picker) and Export CSV button on Products page, csv.ts API client.
<!-- SECTION:FINAL_SUMMARY:END -->
