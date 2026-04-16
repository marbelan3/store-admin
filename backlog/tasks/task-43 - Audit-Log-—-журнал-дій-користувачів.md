---
id: TASK-43
title: Audit Log — журнал дій користувачів
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
Автоматична фіксація хто, коли і що змінив (CRUD товарів, категорій, налаштувань). Критично для мульти-адмін середовища. Реалізація через JPA EntityListener або Spring Events.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Створено entity AuditLog та таблицю audit_logs (Liquibase міграція)
- [x] #2 Автоматично фіксуються: створення, оновлення, видалення товарів
- [x] #3 Фіксуються зміни категорій, тегів, налаштувань тенанта, ролей користувачів
- [x] #4 Кожен запис містить: userId, action, entityType, entityId, changes (JSON diff), timestamp
- [x] #5 GET /api/audit-logs — перегляд з фільтрацією по entity, user, дата
- [x] #6 Сторінка /audit-log на фронтенді з таблицею та фільтрами
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Backend implementation complete. Created migration, entity, repository, service, DTO, controller. Integrated audit logging into ProductService, CategoryService, TagService, UserService. compileJava passes.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Audit Log system.

Backend: AuditLog entity + migration 013, AuditService with REQUIRES_NEW propagation, AuditController GET /api/audit-logs with filters (entityType, userId, dateFrom/dateTo). Integrated into ProductService, CategoryService, TagService, UserService for CREATE/UPDATE/DELETE actions with JSON change diffs.
Frontend: /audit-log page with entity type and action filters, paginated table with expandable JSON changes, added to sidebar nav.
<!-- SECTION:FINAL_SUMMARY:END -->
