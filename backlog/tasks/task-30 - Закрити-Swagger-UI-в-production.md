---
id: TASK-30
title: Закрити Swagger UI в production
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Swagger UI та API docs доступні без автентифікації (permitAll в SecurityConfig). Це розкриває повну карту API для атакуючого.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Swagger UI доступний тільки в dev-профілі або захищений SUPER_ADMIN
- [ ] #2 В production /api-docs/** та /swagger-ui/** повертають 404 або 403
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added app.swagger.enabled config (default false). SecurityConfig conditionally permits Swagger endpoints only when enabled. Set true in application.yml for dev.
<!-- SECTION:FINAL_SUMMARY:END -->
