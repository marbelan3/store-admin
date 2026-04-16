---
id: TASK-44
title: Breadcrumbs навігація
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:02'
labels:
  - frontend
  - ux
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Відсутні breadcrumbs на всіх сторінках. Компонент breadcrumb встановлений в shadcn-svelte, але не використовується. Потрібно для орієнтації (особливо Products > [Name] > Edit).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Breadcrumbs відображаються на всіх сторінках під header
- [x] #2 Dashboard, Products, Products > New, Products > [Name], Categories, Users, Settings
- [x] #3 Використовується shadcn-svelte breadcrumb компонент
- [x] #4 Клікабельні посилання для навігації вгору по ієрархії
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added breadcrumbs navigation to all pages.

Created Breadcrumbs.svelte wrapper using shadcn-svelte breadcrumb components. Applied to: Dashboard, Products, Products/New, Products/[id], Categories, Users, Settings, Audit Log. Last item renders as non-linked page, others as clickable links.
<!-- SECTION:FINAL_SUMMARY:END -->
