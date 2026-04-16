package com.g2u.admin.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.infrastructure.security.JwtTokenProvider;
import com.g2u.admin.service.ProductService;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.ProductDto;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listProducts_withoutAuth_shouldDenyAccess() throws Exception {
        int status = mockMvc.perform(get("/api/products"))
                .andReturn().getResponse().getStatus();
        // OAuth2 login may redirect (302) or return 401/403
        assertTrue(status == 401 || status == 403 || status == 302,
                "Expected 401, 403, or 302, got " + status);
    }

    @Test
    void listProducts_withAuth_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, tenantId, "test@example.com", "TENANT_ADMIN");

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void createProduct_asViewer_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, tenantId, "viewer@example.com", "TENANT_VIEWER");

        CreateProductRequest request = new CreateProductRequest(
                "Test", null, null, null, null, null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpoint_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void authRefresh_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"invalid\"}"))
                .andExpect(status().isUnauthorized());
    }
}
