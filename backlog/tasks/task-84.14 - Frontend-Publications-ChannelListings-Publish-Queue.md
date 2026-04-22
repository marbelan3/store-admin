---
id: TASK-84.14
title: 'Frontend: Publications + ChannelListings + Publish Queue'
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - frontend
  - channels
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Pages: /channels/[id]/publications (product × channel, bulk publish button), /channels/[id]/listings (active listings з статусами і помилками), /channels/publish-queue (stuck jobs, retry, errors). Bulk publish flow.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 /channels/[id]/publications: product picker + bulk publish/unpublish
- [ ] #2 /channels/[id]/listings: table з external URL, status, last_synced, errors
- [ ] #3 /channels/publish-queue: queue jobs з retry/cancel actions
- [ ] #4 Real-time status через SSE або polling
- [ ] #5 Error detail modal показує raw response від каналу
<!-- AC:END -->
