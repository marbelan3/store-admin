---
id: TASK-47
title: Попередження про незбережені зміни
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:30'
updated_date: '2026-04-15 18:03'
labels:
  - frontend
  - ux
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Форми редагування не попереджують при спробі покинути сторінку з незбереженими змінами. Потрібно відстежувати dirty state та показувати діалог.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Форма товару відстежує dirty state (порівняння початкових значень з поточними)
- [x] #2 При навігації зі змінами — діалог 'You have unsaved changes. Discard?'
- [x] #3 Використано beforeNavigate з SvelteKit
- [x] #4 beforeunload для закриття вкладки
- [x] #5 Форма Settings також підтримує dirty state
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added unsaved changes warning to forms.

Created useUnsavedChanges.svelte.ts hook using SvelteKit beforeNavigate + window beforeunload. Tracks dirty state, shows confirm dialog on navigation.
Applied to: ProductForm (oninput/onchange tracking, reset after save), Settings page (compares current vs original values).
<!-- SECTION:FINAL_SUMMARY:END -->
