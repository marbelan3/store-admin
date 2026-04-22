---
id: TASK-82.1
title: 'Design tokens foundation (typography, spacing, shadow, motion, radius)'
status: To Do
assignee: []
created_date: '2026-04-17 15:14'
labels:
  - design-system
  - frontend
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити ad-hoc Tailwind класи на tokens: --text-display/h1/h2/body/sm/xs з tabular-nums, --space-1..8 (8pt scale), --radius-sm/md/lg/pill, --motion-fast/base/slow, --shadow-sm/md/lg. Апдейт app.css + документація у Storybook-like page. Всі сторінки мають використовувати tokens.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Додано CSS custom properties у app.css для typography, spacing, radius, motion, shadow
- [ ] #2 Typography tokens налаштовано з font-feature-settings "tnum" для чисел
- [ ] #3 Шкала spacing дотримується 8pt
- [ ] #4 Existing сторінки (dashboard, products, inventory) не ламаються після міграції
- [ ] #5 Документовано приклади використання
<!-- AC:END -->
