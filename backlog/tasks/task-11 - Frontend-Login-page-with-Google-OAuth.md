---
id: TASK-11
title: 'Frontend: Login page with Google OAuth'
status: Done
assignee: []
created_date: '2026-04-15 11:58'
updated_date: '2026-04-15 12:51'
labels:
  - frontend
  - auth
  - ui
  - phase-1
dependencies:
  - TASK-10
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create login page (src/routes/login/+page.svelte):

- Clean, modern design using shadcn-svelte
- "Continue with Google" button (primary CTA)
- Google icon + brand guidelines
- Redirect to Google OAuth consent screen
- Handle callback, store JWT in httpOnly cookie via SvelteKit server endpoint
- After login: redirect to dashboard or tenant selector
- Error handling for failed OAuth
- Dark/light mode support via mode-watcher
- Responsive (mobile-friendly)

SvelteKit server routes:
- GET /auth/google - redirect to Google
- GET /auth/callback - handle Google callback, exchange code via backend API
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Google OAuth login button works
- [ ] #2 Successful login redirects to dashboard
- [ ] #3 Failed login shows error message
- [ ] #4 JWT stored in httpOnly cookie
- [ ] #5 Responsive design
- [ ] #6 Dark/light mode
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Login page with Google OAuth button, auth callback page for token handling.
<!-- SECTION:FINAL_SUMMARY:END -->
