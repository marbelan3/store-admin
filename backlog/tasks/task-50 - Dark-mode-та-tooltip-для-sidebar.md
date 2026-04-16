---
id: TASK-50
title: Dark mode та tooltip для sidebar
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:31'
updated_date: '2026-04-15 18:13'
labels:
  - frontend
  - ux
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
CSS теми .dark повністю визначені, mode-watcher встановлений, але немає UI для переключення. Також tooltip встановлений, але не використовується для згорнутого sidebar.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Перемикач теми в sidebar footer або user dropdown
- [ ] #2 Вибір теми зберігається в localStorage
- [ ] #3 Tooltip з назвою розділу при наведенні на іконку в згорнутому sidebar
- [ ] #4 Стан sidebar (згорнутий/розгорнутий) зберігається в localStorage
<!-- AC:END -->
