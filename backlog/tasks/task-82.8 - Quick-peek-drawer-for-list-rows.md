---
id: TASK-82.8
title: Quick-peek drawer for list rows
status: To Do
assignee: []
created_date: '2026-04-17 15:19'
labels:
  - frontend
  - ux-pattern
dependencies: []
parent_task_id: TASK-82
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Компонент <QuickPeekDrawer> 560px справа, відкривається при кліку на рядок у списку замість route change. Esc — закрити, Shift+Enter — go to full page. Показує summary + actions. Застосувати на products, orders, customers, marketplace products.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 QuickPeekDrawer.svelte компонент з анімацією slide-in
- [ ] #2 Closes on Esc / backdrop click / route change
- [ ] #3 Shift+Enter → навігація на full detail page
- [ ] #4 Products, Orders, Customers list-сторінки використовують drawer
- [ ] #5 На мобілі: full-screen
<!-- AC:END -->
