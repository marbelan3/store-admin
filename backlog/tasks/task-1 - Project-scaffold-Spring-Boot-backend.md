---
id: TASK-1
title: 'Project scaffold: Spring Boot backend'
status: Done
assignee: []
created_date: '2026-04-15 11:56'
updated_date: '2026-04-15 12:21'
labels:
  - backend
  - setup
  - phase-1
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Initialize Spring Boot 3.3 project with Gradle (Kotlin DSL).

Modules: spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa, spring-boot-starter-oauth2-client, spring-boot-starter-oauth2-resource-server, spring-boot-starter-validation.

Dependencies: MapStruct, Liquibase, MySQL connector, SpringDoc OpenAPI, Lombok.

Package structure: com.g2u.admin with config/, domain/, service/, web/, security/ packages.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Spring Boot 3.3+ project compiles and runs
- [ ] #2 All required dependencies in build.gradle.kts
- [ ] #3 Package structure created
- [ ] #4 application.yml configured for dev profile
<!-- AC:END -->
