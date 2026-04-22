---
id: TASK-82.14
title: Split-pane list+detail view on wide screens (≥1440px)
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux-pattern
dependencies: []
parent_task_id: TASK-82
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
На 1440px+ показати список + detail side-by-side (як Gmail/Attio). Selected row → detail справа 60%. Resize bar. На вузьких екранах — fallback на quick-peek drawer чи route change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Auto-enable при window.innerWidth >= 1440
- [ ] #2 List: 40%, Detail: 60%, resizable з drag handle
- [ ] #3 Selected row highlighted in list
- [ ] #4 URL sync: /products?selected=<id>
- [ ] #5 User toggle у toolbar: Split / List
<!-- AC:END -->
