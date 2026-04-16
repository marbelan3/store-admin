---
id: TASK-25
title: Виправити slug генерацію для української мови
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
Поточний generateSlug() видаляє всі не-ASCII символи regex [^a-z0-9\s-], тому українські назви товарів стають порожнім slug. Потрібна транслітерація UA→Latin. Також slug дублюється в 3 сервісах — винести в SlugUtils.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено утилітний клас SlugUtils.generateSlug() з транслітерацією кирилиці
- [ ] #2 Українська назва 'Футболка жіноча' генерує slug 'futbolka-zhinocha'
- [ ] #3 Видалено дублювання generateSlug() з ProductService, CategoryService, TagService
- [ ] #4 Додано перевірку унікальності slug (existsByTenantIdAndSlug) перед збереженням
- [ ] #5 При конфлікті slug автоматично додається суфікс (-1, -2)
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created SlugUtils with full Ukrainian transliteration (33 chars), ensureUnique() with suffix fallback. Replaced duplicated generateSlug() in 3 services. Slug uniqueness now checked via existsByTenantIdAndSlug before save.
<!-- SECTION:FINAL_SUMMARY:END -->
