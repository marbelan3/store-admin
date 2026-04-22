---
id: TASK-85.13
title: AI Category + Attribute Auto-Mapper
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - ai
  - channels
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
One of painful points - category mapping between us and external channels (Rozetka, Prom have 1000+ categories). Use LLM to suggest best mapping based on product name+description+existing category path. Human confirms. Memoize.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 LlmCategoryMapper service: input (product, channel) -> output (suggested category + confidence)
- [ ] #2 Batch job for all unmapped products in channel
- [ ] #3 UI: bulk review modal - accept/reject/modify suggestions
- [ ] #4 Memoize: if 10 similar products -> 1st suggestion applied to rest (grouped by category+tags)
- [ ] #5 Attribute auto-mapping: our attrs -> channel attrs (color, size, brand)
- [ ] #6 Accuracy metric: track accept rate, improve prompt
<!-- AC:END -->
