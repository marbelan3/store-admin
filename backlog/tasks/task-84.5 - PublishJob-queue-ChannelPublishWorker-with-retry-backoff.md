---
id: TASK-84.5
title: PublishJob queue + ChannelPublishWorker with retry/backoff
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - jobs
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Outbound work queue: publish_jobs (channel_id, publication_id, action, status, attempts, nextAttemptAt, payloadHash, errorJson). Worker polls кожні 30s, claim via optimistic locking, exponential backoff (max 8 attempts).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Liquibase 035-create-publish-jobs.yaml
- [ ] #2 ChannelPublishService.enqueue(publicationId, action)
- [ ] #3 ChannelPublishWorker @Scheduled(fixedDelay=30s)
- [ ] #4 Claim via UPDATE ... WHERE status='PENDING' LIMIT N optimistic lock
- [ ] #5 Retries: 5s, 30s, 2m, 10m, 30m, 2h, 8h, 24h
- [ ] #6 Error logged до channel_sync_logs
- [ ] #7 Admin UI: /channels/publish-queue показує stuck jobs
<!-- AC:END -->
