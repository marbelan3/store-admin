---
id: TASK-84.13
title: 'Frontend: Channels section — connections list + detail'
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
Дзеркалити frontend/src/routes/(app)/marketplace/** як /channels/**. Pages: /channels (list connections з health cards), /channels/new (wizard), /channels/[id] (detail: settings, credentials, test connection).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Route /channels/+page.svelte — grid з health cards (status ring, last sync, error count, 7d throughput mini-chart)
- [ ] #2 Route /channels/new — wizard: choose channel → OAuth або API key → test → name it
- [ ] #3 Route /channels/[id] — tabs Settings / Credentials / Logs / Publications
- [ ] #4 Test Connection button викликає SalesChannelService.test()
- [ ] #5 Sidebar section CHANNELS додано
<!-- AC:END -->
