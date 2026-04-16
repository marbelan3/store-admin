---
id: TASK-51
title: Покращення Dashboard з реальними метриками
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - backend
  - frontend
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Dashboard показує лише лічильники товарів. Потрібні корисні метрики для адмінів: товари з низьким запасом, без зображень, без категорій, нещодавно змінені.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Карточка 'Low Stock' — кількість товарів з quantity <= lowStockThreshold
- [ ] #2 Карточка 'No Images' — товари без жодного медіа
- [ ] #3 Карточка 'Uncategorized' — товари без категорій
- [ ] #4 Таблиця 'Recently Updated' — останні 10 змінених товарів
- [ ] #5 Dashboard помилки відображаються замість мовчки ковтаються
<!-- AC:END -->
