---
id: TASK-19
title: 'Backend: CORS config + frontend-backend integration'
status: Done
assignee: []
created_date: '2026-04-15 12:02'
updated_date: '2026-04-15 12:51'
labels:
  - backend
  - integration
  - phase-1
dependencies:
  - TASK-1
  - TASK-2
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Configure cross-origin communication:

1. Spring CORS config: allow SvelteKit dev server origin
2. Cookie-based auth: SameSite, Secure, httpOnly settings
3. CSRF protection strategy
4. Error response format standardization (JSON error body)
5. Global exception handler (@RestControllerAdvice)
6. Request/response logging for development

Verify full frontend-backend flow works locally.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Frontend can call backend API without CORS errors
- [ ] #2 Cookies work cross-origin in dev
- [ ] #3 Standard error response format
- [ ] #4 Global exception handler catches all errors
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
CORS configured in WebConfig, frontend .env with VITE_API_URL pointing to backend.
<!-- SECTION:FINAL_SUMMARY:END -->
