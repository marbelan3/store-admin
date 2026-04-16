---
id: TASK-24
title: Global Exception Handler (@ControllerAdvice)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 14:28'
updated_date: '2026-04-15 14:44'
labels:
  - backend
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Stack traces витікають клієнту при будь-якій помилці. Потрібен @RestControllerAdvice з обробкою: IllegalArgumentException→400, ResourceNotFoundException→404, AccessDeniedException→403, ConstraintViolationException→409, generic Exception→500 з безпечним повідомленням. Створити ієрархію кастомних exceptions.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Створено GlobalExceptionHandler з @RestControllerAdvice
- [ ] #2 Створено кастомні exceptions: ResourceNotFoundException, DuplicateResourceException, BusinessRuleViolationException
- [ ] #3 IllegalArgumentException повертає 400 без stack trace
- [ ] #4 ConstraintViolationException повертає 409 з описом конфлікту
- [ ] #5 MethodArgumentNotValidException повертає 400 з деталями полів
- [ ] #6 Невідомі помилки повертають 500 з безпечним повідомленням (без стектрейсу)
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created GlobalExceptionHandler with @RestControllerAdvice, custom exceptions (ResourceNotFoundException, DuplicateResourceException, BusinessRuleViolationException), and ErrorResponse record. Updated all services to use ResourceNotFoundException instead of IllegalArgumentException.
<!-- SECTION:FINAL_SUMMARY:END -->
