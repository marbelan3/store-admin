---
id: TASK-83.14
title: CMS pages + navigation + blog
status: To Do
assignee: []
created_date: '2026-04-17 15:21'
labels:
  - backend
  - frontend
  - cms
dependencies: []
parent_task_id: TASK-83
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Зараз немає контент-інструментів. Додати: Page (slug, content rich), BlogPost, NavigationMenu (tree). Simple markdown/Tiptap editor. Storefront serves /pages/{slug}, /blog/{slug}, navigation tree.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Entities: Page, BlogPost, NavigationMenu, MenuItem (tree)
- [ ] #2 Tiptap rich text editor у UI
- [ ] #3 Storefront: GET /api/public/pages/{slug}, /blog, /navigation
- [ ] #4 SEO: meta + slug valid UA/EN
- [ ] #5 UI: /content/pages, /content/blog, /content/navigation
<!-- AC:END -->
