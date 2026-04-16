---
id: TASK-3
title: 'Docker Compose: MySQL + dev environment'
status: Done
assignee: []
created_date: '2026-04-15 11:57'
updated_date: '2026-04-15 12:07'
labels:
  - devops
  - setup
  - phase-1
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create docker-compose.yml with MySQL 8.0+ container.

Services: mysql (port 3306), adminer/phpmyadmin (optional).

Volume for persistent data. Init script for creating database g2u_admin.

Environment variables via .env file (not committed).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 docker-compose up starts MySQL
- [ ] #2 Database g2u_admin created automatically
- [ ] #3 .env.example provided
- [ ] #4 .gitignore includes .env
<!-- AC:END -->
