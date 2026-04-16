---
id: TASK-53
title: Логування та cleanup конфігурації
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - backend
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Жоден клас не має SLF4J Logger. show-sql:true в production. Подвійна CORS конфігурація. Невикористані залежності (oauth2-resource-server, два пакети Lucide).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано SLF4J Logger в контролери та сервіси
- [ ] #2 show-sql та format_sql перенесено в application-dev.yml
- [ ] #3 Створено application-dev.yml та application-prod.yml профілі
- [ ] #4 Видалено подвійну CORS конфігурацію (залишити тільки SecurityConfig)
- [ ] #5 JWT default secret видалено з production профілю
- [ ] #6 Видалено невикористану залежність spring-boot-starter-oauth2-resource-server
<!-- AC:END -->
