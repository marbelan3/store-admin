---
id: TASK-82.2
title: Unified StatusBadge component
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - design-system
  - frontend
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Один компонент для всіх статусів у додатку. Мапа STATUS_COLORS: PENDING→warning, ACTIVE→success, PROCESSING→info, SHIPPED→violet, DELIVERED→success-strong, CANCELLED→danger, ARCHIVED→neutral. Використовуються у products, orders, alerts, sync-logs, channels.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено <StatusBadge type="" /> компонент з варіантами success/warning/danger/info/neutral/violet
- [ ] #2 Мапа статусів у $lib/design/status.ts
- [ ] #3 Замінено всі inline color classes (bg-emerald-100 тощо) на <StatusBadge>
- [ ] #4 Підтримка dark mode
- [ ] #5 З іконкою опціонально
<!-- AC:END -->
