---
id: TASK-82.6
title: Dashboard redesign with trended metrics and sparklines
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - dashboard
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Замінити 10 стат-карток на 4 hero-метрики з sparkline + delta% (Revenue, Orders, Conversion, Active products). Тогл 7d/30d. Замінити fake bar chart (div width * 200) на <Sparkline> / <Donut>. Group "Needs attention" alerts в одну картку. Activity feed внизу.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hero row: 4 колонки з metric + sparkline + delta vs previous period
- [ ] #2 Range toggle: 7d / 30d / 90d, збережено в localStorage
- [ ] #3 Needs Attention card: агрегує low stock, no images, uncategorized, margin alerts
- [ ] #4 Справжня diagram primitive <Sparkline> / <DonutChart>
- [ ] #5 Empty/error states для кожного widget
- [ ] #6 API endpoint повертає трендові дані
<!-- AC:END -->
