---
id: TASK-46
title: Confirm Dialog для деструктивних дій
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
Видалення товарів та категорій використовує нативний confirm(). Компонент Dialog вже є в UI kit. Потрібен переиспользований ConfirmDialog з червоною кнопкою та описом наслідків.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Створено компонент ConfirmDialog на базі shadcn Dialog
- [x] #2 Видалення товару використовує ConfirmDialog
- [x] #3 Видалення категорії використовує ConfirmDialog
- [x] #4 Деактивація користувача використовує ConfirmDialog
- [x] #5 Bulk delete використовує ConfirmDialog з кількістю елементів
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created ConfirmDialog component and integrated for all destructive actions.

ConfirmDialog.svelte: bindable open, title, description, confirmLabel, variant (destructive/default), async onConfirm with loading state. Uses shadcn Dialog.
Applied to: product delete, category delete, user deactivate/activate. Replaced all native confirm() calls.
<!-- SECTION:FINAL_SUMMARY:END -->
