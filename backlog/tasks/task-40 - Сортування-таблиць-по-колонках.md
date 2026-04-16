---
id: TASK-40
title: Сортування таблиць по колонках
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 17:56'
labels:
  - frontend
  - ux
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Заголовки таблиць Products та Users — звичайний текст. Потрібно клікабельні заголовки з індикатором напрямку сортування.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Клік по заголовку колонки змінює sort параметр та перезавантажує дані
- [x] #2 Індикатор напрямку сортування (стрілка) відображається на активній колонці
- [x] #3 Сортування для Products: name, price, status, createdAt
- [x] #4 Сортування для Users: name, email, role, createdAt
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add sortable column headers to Products table (server-side sort via URL params)
2. Add sortable column headers to Users table (client-side sort)
3. Add sort indicators (arrow up/down) on active sort column
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added sortable table columns to Products and Users pages.

Products page: Server-side sorting via sort URL param (name, status, price, createdAt). Clickable headers with arrow indicators.
Users page: Client-side sorting using $derived (name, email, role, createdAt). Same clickable header UX.
Also added search box with 300ms debounce and category filter dropdown to Products page (frontend for TASK-39).
<!-- SECTION:FINAL_SUMMARY:END -->
