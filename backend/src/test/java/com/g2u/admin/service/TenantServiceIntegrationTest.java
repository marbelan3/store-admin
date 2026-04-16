package com.g2u.admin.service;

import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.TenantDto;
import com.g2u.admin.web.dto.UpdateTenantRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TenantServiceIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder()
                .name("Test Store")
                .slug("test-store-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
    }

    @Test
    void getTenant_returnsAllFields() {
        TenantDto dto = tenantService.getTenant(tenantId);

        assertEquals("Test Store", dto.name());
        assertTrue(dto.active());
        assertNull(dto.primaryColor());
        assertNull(dto.taxRate());
        assertNull(dto.businessEmail());
        assertNull(dto.businessPhone());
        assertNull(dto.businessAddress());
        assertNull(dto.website());
    }

    @Test
    void getTenant_notFound_throwsException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> tenantService.getTenant(randomId));
    }

    @Test
    void updateTenant_brandingFields() {
        UpdateTenantRequest request = new UpdateTenantRequest(
                "Updated Store",
                "https://cdn.example.com/logo.png",
                "#4F46E5",
                new BigDecimal("20.00"),
                "contact@store.com",
                "+380501234567",
                "123 Main St, Kyiv, Ukraine",
                "https://store.com",
                Map.of("currency", "UAH")
        );

        TenantDto result = tenantService.updateTenant(tenantId, request);

        assertEquals("Updated Store", result.name());
        assertEquals("https://cdn.example.com/logo.png", result.logoUrl());
        assertEquals("#4F46E5", result.primaryColor());
        assertEquals(new BigDecimal("20.00"), result.taxRate());
        assertEquals("contact@store.com", result.businessEmail());
        assertEquals("+380501234567", result.businessPhone());
        assertEquals("123 Main St, Kyiv, Ukraine", result.businessAddress());
        assertEquals("https://store.com", result.website());
        assertEquals("UAH", result.settings().get("currency"));
    }

    @Test
    void updateTenant_partialUpdate_onlyChangesProvidedFields() {
        // First set some values
        UpdateTenantRequest initial = new UpdateTenantRequest(
                null, null, "#FF0000", null, "first@store.com",
                null, null, null, null
        );
        tenantService.updateTenant(tenantId, initial);

        // Now update only email, color should remain
        UpdateTenantRequest partial = new UpdateTenantRequest(
                null, null, null, null, "second@store.com",
                null, null, null, null
        );
        TenantDto result = tenantService.updateTenant(tenantId, partial);

        assertEquals("#FF0000", result.primaryColor());
        assertEquals("second@store.com", result.businessEmail());
    }
}
