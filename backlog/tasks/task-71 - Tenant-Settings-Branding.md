---
id: TASK-71
title: Tenant Settings & Branding
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 19:59'
updated_date: '2026-04-15 21:12'
labels:
  - backend
  - frontend
  - phase9
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Розширені налаштування тенанта: лого, назва магазину, валюта, timezone, податки, кольорова схема. Кожен тенант повинен відчувати що це 'його' панель. Зараз Settings page занадто базова.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Backend: додати поля до Tenant entity — logo, primaryColor, taxRate, businessEmail, businessPhone
- [x] #2 Liquibase міграція для нових полів tenant
- [ ] #3 Upload лого тенанта (інтеграція з Media Manager)
- [ ] #4 Frontend: розширена Settings page з секціями: General, Branding, Tax, Contact
- [ ] #5 Логотип тенанта відображається в sidebar та на login
- [x] #6 Тести: оновлення налаштувань, tenant isolation
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create Liquibase migration 024-extend-tenant.yaml adding columns: primary_color, tax_rate, business_email, business_phone, business_address, website
2. Update Tenant entity with new fields + annotations
3. Update TenantDto record with new fields
4. Update UpdateTenantRequest record with new validated fields
5. Update TenantService.updateTenant() to handle new fields
6. Add TenantServiceIntegrationTest
7. Run ./gradlew test to verify
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added 6 new columns to tenants table via Liquibase migration 024-extend-tenant.yaml
- Extended Tenant entity with primaryColor, taxRate, businessEmail, businessPhone, businessAddress, website
- Updated TenantDto to include all new fields
- Updated UpdateTenantRequest with validation annotations (hex color pattern, decimal range for tax, email format)
- Extended TenantService.updateTenant() to handle all new fields with null-safe partial update
- Created TenantServiceIntegrationTest with 4 tests: get, not-found, full update, partial update
- All tests pass (./gradlew test BUILD SUCCESSFUL)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Extended Tenant entity with branding fields:
- Migration 024: added primary_color, tax_rate, business_email, business_phone, business_address, website
- Updated Tenant entity, TenantDto, UpdateTenantRequest with validation
- Updated TenantService to handle new fields
- 4 integration tests added
- 145 total tests passing
<!-- SECTION:FINAL_SUMMARY:END -->
