---
id: TASK-85.16
title: One-click Rozetka Launch Wizard (onboarding)
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - frontend
  - ukraine
  - onboarding
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Killer-feature for UA sellers: wizard /channels/rozetka/launch walks through 5 steps: (1) API credentials, (2) feed URL setup, (3) category auto-mapping (AI-assist), (4) NP delivery setup, (5) Checkbox PRRO binding. End-to-end in 10 min instead of 2 days.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Wizard route /channels/rozetka/launch with 5-step stepper
- [ ] #2 Step 1: API Token input + validation
- [ ] #3 Step 2: Feed URL + schedule (testing with Rozetka validator)
- [ ] #4 Step 3: AI category mapping bulk (uses TASK from AI Category Mapper)
- [ ] #5 Step 4: Nova Poshta credentials + warehouse selection
- [ ] #6 Step 5: Checkbox API key + test receipt
- [ ] #7 After finish: bulk publish top 50 products
- [ ] #8 Progress saved at each step (resume later)
<!-- AC:END -->
