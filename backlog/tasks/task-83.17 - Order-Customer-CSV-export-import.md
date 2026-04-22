---
id: TASK-83.17
title: Order & Customer CSV export + import
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - csv
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ProductCsvController вже є. Додати OrderCsvController, CustomerCsvController, InventoryCsvController. Export: всі поля + relations flatten. Import: dry-run, error report, batch processing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 OrderCsvController: POST /import, GET /export
- [ ] #2 CustomerCsvController: POST /import, GET /export
- [ ] #3 InventoryCsvController: POST /import (quantity updates), GET /export
- [ ] #4 Dry-run mode повертає помилки без змін
- [ ] #5 Frontend: Export button на list pages з filters applied
<!-- AC:END -->
