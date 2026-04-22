---
id: TASK-85.3
title: Checkbox PRRO fiscalization — post-payment receipts
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - fiscalization
  - ukraine
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Checkbox API for receipt fiscalization (PRRO). After payment.captured - auto-send receipt. Types: sale, return. PDV rates 20%/7%/0%. Sign via KEP key or API token.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entity FiscalReceipt with externalReceiptId, status, pdfUrl
- [ ] #2 CheckboxPrroAdapter: sendSale, sendReturn, getReceipt
- [ ] #3 Auto-trigger after payment.captured event
- [ ] #4 Receipt PDF URL stored + exposed in Order detail
- [ ] #5 Error handling: if Checkbox down, queue for retry
- [ ] #6 PDV rates mapping: 20% / 7% / 0% per line item
- [ ] #7 Admin UI: /settings/fiscalization - cashier registration, test receipt
<!-- AC:END -->
