---
id: TASK-55
title: Сповіщення для адмінів (In-app Notifications)
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
Система нотифікацій в інтерфейсі: низький залишок товару, масовий імпорт завершено, новий користувач. Polling або SSE.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено entity Notification та таблицю notifications
- [ ] #2 Bell icon з лічильником в navigation header
- [ ] #3 Dropdown з останніми нотифікаціями
- [ ] #4 Нотифікація при low stock (quantity <= threshold)
- [ ] #5 Нотифікація при новому користувачі (OAuth login)
- [ ] #6 Можливість позначити як прочитане / прочитати всі
<!-- AC:END -->
