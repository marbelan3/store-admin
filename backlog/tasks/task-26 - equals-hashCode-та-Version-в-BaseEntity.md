---
id: TASK-26
title: equals/hashCode та @Version в BaseEntity
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:28'
updated_date: '2026-04-15 14:44'
labels:
  - backend
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
JPA entities не мають equals/hashCode — баги в Set колекціях (Product.categories, Product.tags). Також відсутній @Version для optimistic locking — lost updates при конкурентному редагуванні.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано equals() та hashCode() в BaseEntity на основі id з null-safe перевіркою
- [ ] #2 Додано @Version private Long version в BaseEntity
- [ ] #3 Створено Liquibase міграцію для додавання колонки version до всіх таблиць
- [ ] #4 Set колекції в Product коректно працюють з detached entities
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added equals/hashCode to BaseEntity (null-safe, Hibernate best practice). Added @Version for optimistic locking. Liquibase migration 010 adds version column to all 7 tables.
<!-- SECTION:FINAL_SUMMARY:END -->
