---
id: TASK-33
title: Пагінація списку товарів на фронтенді
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:29'
updated_date: '2026-04-15 15:20'
labels:
  - frontend
  - ux
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Бекенд повертає Page<T> з totalPages, totalElements, first, last, але фронтенд ігнорує — видно тільки перші 20 товарів без кнопок навігації. Також фільтри не зберігаються в URL.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано компонент пагінації під таблицею (Previous/Next, Page X of Y)
- [ ] #2 Фільтр статусу та номер сторінки синхронізуються з URL query params (?page=2&status=ACTIVE)
- [ ] #3 Кнопка Back браузера коректно працює з фільтрами та пагінацією
- [ ] #4 При зміні фільтра сторінка скидається на першу
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added pagination controls (Previous/Next, Page X of Y, item count) to products page. Filters and page synced to URL query params (?page=1&status=ACTIVE). Filter change resets to page 1.
<!-- SECTION:FINAL_SUMMARY:END -->
