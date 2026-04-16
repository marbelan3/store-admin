---
id: TASK-28
title: Очищення TenantContext та SecurityContext в фільтрах
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:28'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TenantContext (ThreadLocal) не очищується при помилках в JwtAuthenticationFilter. Tomcat пул потоків може передати чужий tenantId наступному запиту. Також JwtAuthenticationFilter не очищує SecurityContext при невалідному токені.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 JwtAuthenticationFilter очищує TenantContext в finally блоці після filterChain.doFilter()
- [ ] #2 SecurityContext коректно очищується для невалідних токенів
- [ ] #3 Написано тест що підтверджує очищення контексту
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Wrapped filterChain.doFilter() in try/finally with TenantContext.clear() in JwtAuthenticationFilter.
<!-- SECTION:FINAL_SUMMARY:END -->
