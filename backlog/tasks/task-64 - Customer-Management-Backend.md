---
id: TASK-64
title: Customer Management (Backend)
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:58'
updated_date: '2026-04-15 20:40'
labels:
  - backend
  - phase7
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Управління клієнтами: профілі, контактні дані, історія замовлень, нотатки. Третій ключовий модуль e-commerce трійки (Products-Orders-Customers).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Entity Customer (id, tenantId, firstName, lastName, email, phone, notes, totalOrders, totalSpent, createdAt)
- [x] #2 Entity CustomerAddress (id, customerId, type, line1, line2, city, state, postalCode, country, isDefault)
- [x] #3 Liquibase міграції для customers та customer_addresses
- [x] #4 CustomerService: CRUD, search by name/email, link to orders
- [x] #5 REST endpoints: CRUD /api/customers, GET /api/customers/{id}/orders
- [x] #6 GET /api/customers з фільтрами: search, dateRange, пагінація, сортування
- [x] #7 Автоматичне оновлення totalOrders/totalSpent при створенні замовлення
- [x] #8 Tenant isolation: клієнти належать тенанту
- [x] #9 Тести: CRUD, search, tenant isolation, order linkage
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create Liquibase migration 022-create-customers.yaml (customers, customer_addresses tables + customer_id FK on orders)
2. Create Customer entity in domain/customer package
3. Create CustomerAddress entity
4. Add customer_id field to Order entity
5. Create CustomerRepository and CustomerAddressRepository
6. Create DTOs (CustomerDto, CustomerListDto, CustomerAddressDto, Create/Update requests)
7. Create CustomerMapper (MapStruct)
8. Create CustomerService with CRUD, search, address management, order stats
9. Create CustomerController with all REST endpoints
10. Update DashboardService/DashboardStatsDto with totalCustomers
11. Register migration in master.yaml
12. Create CustomerServiceIntegrationTest
13. Run ./gradlew test to verify
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented Customer Management backend:
- Customer + CustomerAddress entities
- Migration 022 for customers, customer_addresses, + customer_id FK on orders
- CustomerService: CRUD, search by name/email, address management
- CustomerController: full REST API with addresses
- MapStruct mapper
- Dashboard updated with totalCustomers
- 9 integration tests, 128 total passing
<!-- SECTION:FINAL_SUMMARY:END -->
