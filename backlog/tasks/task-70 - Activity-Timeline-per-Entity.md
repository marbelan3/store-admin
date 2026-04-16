---
id: TASK-70
title: Activity Timeline per Entity
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 21:03'
labels:
  - frontend
  - backend
  - phase9
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Контекстна історія змін на сторінці товару/замовлення/клієнта (хто змінив що і коли). Відрізняється від глобального audit log — тут inline timeline прямо на detail page. Shopify і BigCommerce обидва використовують цей патерн.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Компонент ActivityTimeline: вертикальний timeline з іконками, описом дії, часом, автором
- [x] #2 Backend endpoint: GET /api/audit-logs?entityType=PRODUCT&entityId={id}
- [x] #3 Інтеграція на сторінку товару: вкладка або секція Activity
- [x] #4 Інтеграція на детальну сторінку замовлення: order status history timeline
- [x] #5 Різні іконки та кольори для різних типів дій (created, updated, status_changed, deleted)
- [x] #6 Relative time display (5 minutes ago, yesterday, etc.)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add entityId filter to backend: AuditController, AuditService, AuditLogRepository
2. Add entityId param to frontend audit API client
3. Create relativeTime utility function
4. Create ActivityTimeline.svelte component
5. Integrate into product detail page
6. Integrate into customer detail page
7. Assess order page timeline (uses OrderStatusHistory, not audit logs)
8. Run svelte-check
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Backend: Added entityId filter to AuditController, AuditService, AuditLogRepository query, and updated integration test calls
- Frontend API: Added entityId to AuditLogFilters and getAuditLogs
- Created relativeTime utility in utils/relativeTime.ts
- Created ActivityTimeline.svelte component with action-based icons/colors, expandable change details, and "show more" pagination
- Integrated into product detail page (bottom section card)
- Integrated into customer detail page (right column card)
- Order page: left as-is — uses OrderStatusHistory (domain-specific status transitions), not audit logs. The existing timeline is better suited for order workflow.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Activity Timeline:
- ActivityTimeline component: vertical timeline with colored dots, icons, relative time, expandable JSON diffs
- Backend: added entityId filter to audit log API
- Integrated on product detail page and customer detail page
- Order detail page kept its own OrderStatusHistory timeline
- relativeTime utility function
- svelte-check: 0 errors
<!-- SECTION:FINAL_SUMMARY:END -->
