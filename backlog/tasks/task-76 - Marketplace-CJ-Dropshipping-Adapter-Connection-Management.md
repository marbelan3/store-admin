---
id: TASK-76
title: 'Marketplace: CJ Dropshipping Adapter & Connection Management'
status: Done
assignee:
  - '@claude'
created_date: '2026-04-15 21:46'
updated_date: '2026-04-16 06:12'
labels:
  - backend
  - marketplace
dependencies:
  - TASK-75
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Adapter pattern для CJ Dropshipping API. MarketplaceAdapter interface + CjDropshippingAdapter implementation. Управління connections з encrypted credentials. Пастка: token має TTL 15 днів, потрібен auto-refresh.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MarketplaceAdapter interface: authenticate(), searchProducts(), getProductDetails(), getVariants(), getStock(), calculateShipping(), placeOrder(), getOrderStatus(), getTracking()
- [x] #2 CjDropshippingAdapter implementation з base URL https://developers.cjdropshipping.com/api2.0/v1
- [x] #3 Token management: auto-refresh accessToken (TTL 15 днів), зберігання в marketplace_connections.access_token
- [x] #4 Rate limiting: respect 1-6 req/sec залежно від рівня, use token bucket or semaphore
- [x] #5 MarketplaceConnectionService: CRUD connections, test connectivity, encrypt/decrypt API keys
- [x] #6 REST endpoints: POST/GET/PUT/DELETE /api/marketplace/connections, POST /connections/{id}/test
- [x] #7 Error handling: CJ API errors mapped to domain exceptions (CjApiException, CjRateLimitException, CjAuthException)
- [x] #8 Integration test: mock CJ API responses, test token refresh flow
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create CJ API exception classes
2. Create MarketplaceAdapter interface
3. Create CjDropshippingAdapter with RestTemplate, token management, rate limiting
4. Create DTOs for connection CRUD
5. Create MarketplaceConnectionService
6. Create MarketplaceConnectionController
7. Integration test with mocked CJ responses
8. Compile & test
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
CJ Dropshipping adapter with connection management.

Changes:
- MarketplaceAdapter interface: 9 methods (authenticate, searchProducts, getProductDetails, getVariants, getStock, calculateShipping, placeOrder, getOrderStatus, getTracking) with inner DTOs
- CjDropshippingAdapter: full implementation with rate limiting (Semaphore-based 1 req/sec), token management, CJ API response parsing, error mapping
- Exception hierarchy: CjApiException, CjAuthException, CjRateLimitException — handled in GlobalExceptionHandler (429, 502)
- MarketplaceConnectionService: CRUD, test connectivity, auto-refresh tokens (15-day TTL, refresh when <1 day remaining)
- MarketplaceConnectionController: REST at /api/marketplace/connections with test endpoint
- DTOs: MarketplaceConnectionDto, CreateMarketplaceConnectionRequest, UpdateMarketplaceConnectionRequest
- 9 integration tests: create, duplicate prevention, auth failure, tenant isolation, update, delete, test connection (success + failure)

Tests: all 174 tests pass
<!-- SECTION:FINAL_SUMMARY:END -->
