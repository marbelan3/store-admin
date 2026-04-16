package com.g2u.admin.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2u.admin.infrastructure.security.JwtTokenProvider;
import com.g2u.admin.web.dto.CreateCategoryRequest;
import com.g2u.admin.web.dto.CreateCustomerRequest;
import com.g2u.admin.web.dto.CreateDiscountRequest;
import com.g2u.admin.web.dto.UpdateCategoryRequest;
import com.g2u.admin.web.dto.UpdateUserRoleRequest;
import com.g2u.admin.domain.discount.DiscountType;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security integration tests verifying authentication and
 * role-based access control across all API endpoints.
 *
 * <p>These tests exercise the real Spring Security filter chain (JWT auth,
 * API key auth, method-level @PreAuthorize) against the full application
 * context to ensure security rules are enforced end-to-end.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------

    private String tokenForRole(String role) {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        return jwtTokenProvider.generateAccessToken(userId, tenantId, role.toLowerCase() + "@test.com", role);
    }

    private String viewerToken() {
        return tokenForRole("TENANT_VIEWER");
    }

    private String adminToken() {
        return tokenForRole("TENANT_ADMIN");
    }

    /**
     * Asserts that the HTTP status indicates access was denied.
     * OAuth2 configurations may produce a 302 redirect instead of a
     * plain 401 or 403, so we accept all three.
     */
    private static void assertDenied(int status, String context) {
        assertTrue(status == 401 || status == 403 || status == 302,
                context + " - expected 401, 403, or 302 but got " + status);
    }

    // ===============================================================
    // 1. Unauthenticated access
    // ===============================================================

    @Nested
    @DisplayName("Unauthenticated access")
    class UnauthenticatedAccessTests {

        // --- Public endpoints (should be accessible) ---

        @Test
        @DisplayName("GET /api/health is public and returns 200")
        void healthEndpoint_shouldBePublic() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));
        }

        @Test
        @DisplayName("POST /api/auth/refresh is public (returns 401 for invalid token, not redirect)")
        void authRefresh_shouldBePublic() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"invalid-token\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // --- Protected endpoints (should be denied without auth) ---

        @Test
        @DisplayName("GET /api/products requires authentication")
        void products_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/products"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/products");
        }

        @Test
        @DisplayName("GET /api/categories requires authentication")
        void categories_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/categories"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/categories");
        }

        @Test
        @DisplayName("POST /api/categories requires authentication")
        void createCategory_withoutAuth_shouldDenyAccess() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Test Category", null, null, null, null, null, null, null);
            int status = mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "POST /api/categories");
        }

        @Test
        @DisplayName("GET /api/users requires authentication")
        void users_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/users"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/users");
        }

        @Test
        @DisplayName("GET /api/audit-logs requires authentication")
        void auditLogs_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/audit-logs"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/audit-logs");
        }

        @Test
        @DisplayName("GET /api/notifications requires authentication")
        void notifications_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/notifications"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/notifications");
        }

        @Test
        @DisplayName("POST /api/media/upload requires authentication")
        void mediaUpload_withoutAuth_shouldDenyAccess() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", MediaType.IMAGE_PNG_VALUE, "fake-image".getBytes());
            int status = mockMvc.perform(multipart("/api/media/upload").file(file))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "POST /api/media/upload");
        }

        @Test
        @DisplayName("GET /api/inventory requires authentication")
        void inventory_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/inventory"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/inventory");
        }

        @Test
        @DisplayName("GET /api/webhooks requires authentication")
        void webhooks_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/webhooks"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/webhooks");
        }
    }

    // ===============================================================
    // 2. TENANT_VIEWER RBAC (should be denied write/admin operations)
    // ===============================================================

    @Nested
    @DisplayName("TENANT_VIEWER role restrictions")
    class TenantViewerRbacTests {

        @Test
        @DisplayName("TENANT_VIEWER cannot POST /api/categories")
        void viewer_cannotCreateCategory() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Viewer Category", null, null, null, null, null, null, null);

            mockMvc.perform(post("/api/categories")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot PUT /api/categories/{id}")
        void viewer_cannotUpdateCategory() throws Exception {
            UUID categoryId = UUID.randomUUID();
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Updated", null, null, null, null, null, null, null, null);

            mockMvc.perform(put("/api/categories/" + categoryId)
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot DELETE /api/categories/{id}")
        void viewer_cannotDeleteCategory() throws Exception {
            UUID categoryId = UUID.randomUUID();

            mockMvc.perform(delete("/api/categories/" + categoryId)
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot POST /api/media/upload")
        void viewer_cannotUploadMedia() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", MediaType.IMAGE_PNG_VALUE, "fake-image".getBytes());

            mockMvc.perform(multipart("/api/media/upload").file(file)
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot GET /api/users (admin only)")
        void viewer_cannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot PATCH /api/users/{id}/role (admin only)")
        void viewer_cannotUpdateUserRole() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRoleRequest request = new UpdateUserRoleRequest("TENANT_ADMIN");

            mockMvc.perform(patch("/api/users/" + userId + "/role")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot GET /api/audit-logs (admin only)")
        void viewer_cannotAccessAuditLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }
    }

    // ===============================================================
    // 3. TENANT_ADMIN RBAC (should have access)
    // ===============================================================

    @Nested
    @DisplayName("TENANT_ADMIN role access")
    class TenantAdminRbacTests {

        @Test
        @DisplayName("TENANT_ADMIN can POST /api/products")
        void admin_canCreateProduct() throws Exception {
            // Even though the product won't be persisted (no real data),
            // we verify the security layer allows the request through.
            // A 400 (validation) or 200/201 means security passed; 403 means it didn't.
            int status = mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Security Test Product\"}"))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on POST /api/products, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can GET /api/users")
        void admin_canListUsers() throws Exception {
            int status = mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/users, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can GET /api/audit-logs")
        void admin_canAccessAuditLogs() throws Exception {
            int status = mockMvc.perform(get("/api/audit-logs")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/audit-logs, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can GET /api/notifications")
        void admin_canAccessNotifications() throws Exception {
            int status = mockMvc.perform(get("/api/notifications")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/notifications, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can GET /api/products")
        void admin_canListProducts() throws Exception {
            mockMvc.perform(get("/api/products")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TENANT_ADMIN can GET /api/categories")
        void admin_canListCategories() throws Exception {
            int status = mockMvc.perform(get("/api/categories")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/categories, got " + status);
        }
    }

    // ===============================================================
    // 4. Storefront API key authentication
    // ===============================================================

    @Nested
    @DisplayName("Storefront API key authentication")
    class StorefrontApiKeyTests {

        @Test
        @DisplayName("GET /api/v1/storefront/products without API key returns 401")
        void storefront_withoutApiKey_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/storefront/products"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/storefront/products with invalid API key returns 401")
        void storefront_withInvalidApiKey_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/storefront/products")
                            .header("X-API-Key", "invalid-api-key-12345"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/storefront/categories without API key returns 401")
        void storefrontCategories_withoutApiKey_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/storefront/categories"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/storefront/products/{slug} with invalid API key returns 401")
        void storefrontProductDetail_withInvalidApiKey_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/storefront/products/some-product")
                            .header("X-API-Key", "bogus-key-value"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Storefront endpoints are not accessible via JWT token (separate filter chain)")
        void storefront_withJwtToken_shouldReturn401() throws Exception {
            // The storefront filter chain uses API key auth, not JWT.
            // A JWT Bearer token should not grant access.
            mockMvc.perform(get("/api/v1/storefront/products")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===============================================================
    // 5. Cross-cutting security concerns
    // ===============================================================

    @Nested
    @DisplayName("Cross-cutting security")
    class CrossCuttingSecurityTests {

        @Test
        @DisplayName("Expired or malformed JWT token is rejected")
        void malformedToken_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/products")
                            .header("Authorization", "Bearer not-a-valid-jwt"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/products with malformed token");
        }

        @Test
        @DisplayName("Missing Bearer prefix is treated as unauthenticated")
        void missingBearerPrefix_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/products")
                            .header("Authorization", adminToken()))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/products without Bearer prefix");
        }

        @Test
        @DisplayName("TENANT_VIEWER can read products (read access is allowed)")
        void viewer_canReadProducts() throws Exception {
            mockMvc.perform(get("/api/products")
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot create products (write access denied)")
        void viewer_cannotCreateProducts() throws Exception {
            int status = mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Forbidden Product\"}"))
                    .andReturn().getResponse().getStatus();
            assertTrue(status == 403,
                    "TENANT_VIEWER should get 403 on POST /api/products, got " + status);
        }
    }

    // ===============================================================
    // 6. Orders endpoint security
    // ===============================================================

    @Nested
    @DisplayName("Orders endpoint security")
    class OrdersSecurityTests {

        @Test
        @DisplayName("GET /api/orders requires authentication")
        void orders_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/orders"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/orders");
        }

        @Test
        @DisplayName("POST /api/orders requires authentication")
        void createOrder_withoutAuth_shouldDenyAccess() throws Exception {
            String itemId = UUID.randomUUID().toString();
            String body = "{\"customerName\":\"Test\",\"totalAmount\":10.00," +
                    "\"items\":[{\"productId\":\"" + itemId + "\",\"productName\":\"Widget\",\"quantity\":1,\"unitPrice\":10.00}]}";
            int status = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "POST /api/orders");
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot POST /api/orders")
        void viewer_cannotCreateOrder() throws Exception {
            // Send a valid body so that validation passes and @PreAuthorize is reached
            String itemId = UUID.randomUUID().toString();
            String body = "{\"customerName\":\"Test\",\"totalAmount\":10.00," +
                    "\"items\":[{\"productId\":\"" + itemId + "\",\"productName\":\"Widget\",\"quantity\":1,\"unitPrice\":10.00}]}";
            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot PUT /api/orders/{id}/status")
        void viewer_cannotUpdateOrderStatus() throws Exception {
            UUID orderId = UUID.randomUUID();
            mockMvc.perform(put("/api/orders/" + orderId + "/status")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"CONFIRMED\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_ADMIN can access GET /api/orders")
        void admin_canListOrders() throws Exception {
            int status = mockMvc.perform(get("/api/orders")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/orders, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can POST /api/orders")
        void admin_canCreateOrder() throws Exception {
            String itemId = UUID.randomUUID().toString();
            String body = "{\"customerName\":\"Test\",\"totalAmount\":10.00," +
                    "\"items\":[{\"productId\":\"" + itemId + "\",\"productName\":\"Widget\",\"quantity\":1,\"unitPrice\":10.00}]}";
            int status = mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on POST /api/orders, got " + status);
        }
    }

    // ===============================================================
    // 7. Customers endpoint security
    // ===============================================================

    @Nested
    @DisplayName("Customers endpoint security")
    class CustomersSecurityTests {

        @Test
        @DisplayName("GET /api/customers requires authentication")
        void customers_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/customers"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/customers");
        }

        @Test
        @DisplayName("POST /api/customers requires authentication")
        void createCustomer_withoutAuth_shouldDenyAccess() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "John", "Doe", "john@test.com", null, null);
            int status = mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "POST /api/customers");
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot POST /api/customers")
        void viewer_cannotCreateCustomer() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "John", "Doe", "john@test.com", null, null);
            mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot PUT /api/customers/{id}")
        void viewer_cannotUpdateCustomer() throws Exception {
            UUID customerId = UUID.randomUUID();
            mockMvc.perform(put("/api/customers/" + customerId)
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Jane\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot DELETE /api/customers/{id}")
        void viewer_cannotDeleteCustomer() throws Exception {
            UUID customerId = UUID.randomUUID();
            mockMvc.perform(delete("/api/customers/" + customerId)
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_ADMIN can access GET /api/customers")
        void admin_canListCustomers() throws Exception {
            int status = mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/customers, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can POST /api/customers")
        void admin_canCreateCustomer() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "John", "Doe", "john@test.com", null, null);
            int status = mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on POST /api/customers, got " + status);
        }
    }

    // ===============================================================
    // 8. Discounts endpoint security
    // ===============================================================

    @Nested
    @DisplayName("Discounts endpoint security")
    class DiscountsSecurityTests {

        @Test
        @DisplayName("GET /api/discounts requires authentication")
        void discounts_withoutAuth_shouldDenyAccess() throws Exception {
            int status = mockMvc.perform(get("/api/discounts"))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "GET /api/discounts");
        }

        @Test
        @DisplayName("POST /api/discounts requires authentication")
        void createDiscount_withoutAuth_shouldDenyAccess() throws Exception {
            CreateDiscountRequest request = new CreateDiscountRequest(
                    "Test Discount", "TEST10", DiscountType.PERCENTAGE,
                    BigDecimal.TEN, null, null, null, null, null, null);
            int status = mockMvc.perform(post("/api/discounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getStatus();
            assertDenied(status, "POST /api/discounts");
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot POST /api/discounts")
        void viewer_cannotCreateDiscount() throws Exception {
            CreateDiscountRequest request = new CreateDiscountRequest(
                    "Test Discount", "TEST10", DiscountType.PERCENTAGE,
                    BigDecimal.TEN, null, null, null, null, null, null);
            mockMvc.perform(post("/api/discounts")
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot PUT /api/discounts/{id}")
        void viewer_cannotUpdateDiscount() throws Exception {
            UUID discountId = UUID.randomUUID();
            mockMvc.perform(put("/api/discounts/" + discountId)
                            .header("Authorization", "Bearer " + viewerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_VIEWER cannot DELETE /api/discounts/{id}")
        void viewer_cannotDeleteDiscount() throws Exception {
            UUID discountId = UUID.randomUUID();
            mockMvc.perform(delete("/api/discounts/" + discountId)
                            .header("Authorization", "Bearer " + viewerToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TENANT_ADMIN can access GET /api/discounts")
        void admin_canListDiscounts() throws Exception {
            int status = mockMvc.perform(get("/api/discounts")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on GET /api/discounts, got " + status);
        }

        @Test
        @DisplayName("TENANT_ADMIN can POST /api/discounts")
        void admin_canCreateDiscount() throws Exception {
            CreateDiscountRequest request = new CreateDiscountRequest(
                    "Test Discount", "ADMINTEST", DiscountType.PERCENTAGE,
                    BigDecimal.TEN, null, null, null, null, null, null);
            int status = mockMvc.perform(post("/api/discounts")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getStatus();
            assertTrue(status != 403 && status != 401,
                    "TENANT_ADMIN should not get 401/403 on POST /api/discounts, got " + status);
        }
    }
}
