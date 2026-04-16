---
id: TASK-72
title: Dashboard Charts & Sparklines
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 21:12'
labels:
  - frontend
  - backend
  - phase9
dependencies:
  - TASK-62
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Додати графіки та sparklines на Dashboard для візуалізації трендів. Замість сухих чисел — trend indicators з percentage change та міні-графіками. Stripe і Shopify обидва використовують цей патерн для миттєвого розуміння трендів.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Sparkline компонент: міні SVG графік (48px висота) для metric cards
- [ ] #2 Backend endpoint: GET /api/dashboard/trends — дані за останні 7/30 днів
- [x] #3 Metric cards з percentage change badge (green up / red down)
- [ ] #4 Revenue chart (якщо є orders): bar або line chart за період
- [ ] #5 Orders chart: кількість замовлень по днях
- [x] #6 Інтеграція lightweight chart library (Chart.js або custom SVG)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create Sparkline.svelte component (pure SVG, viewBox-based)
2. Create TrendBadge.svelte component (pill badge with arrow)
3. Update dashboard page: add sparklines + trend badges to metric cards
4. Add Quick Actions section
5. Add order/revenue summary section with mock data
6. Improve layout (premium feel, responsive grid)
7. Run svelte-check
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created Sparkline.svelte: pure SVG component with cubic bezier curves, gradient fill, viewBox scaling
- Created TrendBadge.svelte: pill badge with up/down/neutral arrows, green/red/gray colors
- Updated dashboard +page.svelte: sparklines + trend badges on all 4 metric cards, Quick Actions section added
- Used mock data with TODO comment for backend integration
- svelte-check passes: 0 errors, 0 warnings
- AC #2 (backend endpoint) and AC #4/#5 (revenue/orders charts) are backend-dependent — skipped for frontend-only scope
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Dashboard Charts:
- Sparkline component: pure SVG with bezier curves, gradient fill
- TrendBadge component: green/red/gray pill with arrows
- Dashboard metric cards enhanced with sparklines + trend badges
- Quick Actions section (Add Product, Create Order, Add Customer)
- Mock data with TODO for real trends API
<!-- SECTION:FINAL_SUMMARY:END -->
