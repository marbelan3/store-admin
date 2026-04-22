---
id: TASK-83.15
title: Reviews + Q&A system
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - catalog
dependencies: []
parent_task_id: TASK-83
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Для продуктів: Review (rating 1-5, title, body, author, photos, verified_purchase, approved). Q&A (question + answers, best answer). Moderation UI. Впливає на product.averageRating.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Review, ReviewMedia, Question, Answer
- [ ] #2 Anti-spam: verified_purchase флаг, rate limiting
- [ ] #3 Moderation: APPROVED/PENDING/SPAM
- [ ] #4 Product.averageRating + reviewCount auto-updated
- [ ] #5 UI: /products/{id}/reviews tab
<!-- AC:END -->
