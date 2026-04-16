---
id: TASK-31
title: Refresh token rotation та відкликання
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:20'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Refresh token — чистий JWT без серверного стану. Немає ротації, відкликання чи blacklist. Скомпрометований refresh token дає доступ на 7 днів. При logout очищується тільки localStorage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Створено таблицю refresh_tokens з Liquibase міграцією
- [x] #2 При refresh — старий токен видаляється, видається новий (rotation)
- [x] #3 При logout — всі refresh tokens користувача видаляються
- [x] #4 Невалідний/відкликаний refresh token повертає 401
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created refresh_tokens table (migration 011), RefreshToken entity, RefreshTokenRepository, TokenHashUtil (SHA-256). AuthController refresh endpoint now validates hash in DB, rotates tokens. Added POST /api/auth/logout that revokes all user tokens. OAuth2SuccessHandler stores hash on login. Frontend logout calls API before clearing localStorage.
<!-- SECTION:FINAL_SUMMARY:END -->
