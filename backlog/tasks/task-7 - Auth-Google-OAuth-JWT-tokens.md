---
id: TASK-7
title: 'Auth: Google OAuth + JWT tokens'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:51'
labels:
  - backend
  - security
  - auth
  - phase-1
dependencies:
  - TASK-4
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement authentication flow:

Backend (Spring Security):
1. Spring Security OAuth2 Client with Google provider
2. OAuth2 callback handler: exchange code for Google tokens
3. Extract Google profile (email, name, avatar)
4. Create or find User in MySQL by oauth_provider_id
5. Generate JWT (access token 15min + refresh token 7d)
6. JWT contains: userId, email, tenantId, role
7. Refresh token endpoint
8. Logout endpoint (invalidate refresh token)

Google Cloud Console:
- Configure OAuth consent screen
- Create OAuth 2.0 Client ID
- Set redirect URIs

Invite flow: user without tenant assignment sees "pending" state until invited.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Google OAuth login flow works end-to-end
- [ ] #2 JWT access + refresh tokens issued
- [ ] #3 User created in DB on first Google login
- [ ] #4 Refresh token rotation works
- [ ] #5 Logout invalidates tokens
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Google OAuth2 flow with OAuth2SuccessHandler, JWT access+refresh tokens via JwtTokenProvider, JwtAuthenticationFilter, AuthController with /api/auth/me and /api/auth/refresh endpoints.
<!-- SECTION:FINAL_SUMMARY:END -->
