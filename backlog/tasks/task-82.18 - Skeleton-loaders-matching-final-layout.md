---
id: TASK-82.18
title: Skeleton loaders matching final layout
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux
dependencies: []
parent_task_id: TASK-82
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Поточні skeleton-и не відображають фінальний layout: dashboard показує 4 плоских картки коли буде 10+ + table, products пропускає PageHeader. Fix so loading state виглядає як target UI. Reduce layout shift.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Dashboard skeleton: 4 hero metrics + needs-attention + activity
- [ ] #2 Products skeleton: header + filter bar + table rows
- [ ] #3 Orders skeleton: filter bar + table rows + pagination placeholder
- [ ] #4 Marketplace products skeleton матчить реальний layout
- [ ] #5 Shimmer animation однаковий по всіх skeleton-ах
<!-- AC:END -->
