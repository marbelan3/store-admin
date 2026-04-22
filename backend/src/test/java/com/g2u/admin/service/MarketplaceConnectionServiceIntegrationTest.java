package com.g2u.admin.service;

import com.g2u.admin.config.TestMarketplaceConfig;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.infrastructure.marketplace.CjAuthException;
import com.g2u.admin.web.dto.CreateMarketplaceConnectionRequest;
import com.g2u.admin.web.dto.MarketplaceConnectionDto;
import com.g2u.admin.web.dto.UpdateMarketplaceConnectionRequest;
import com.g2u.admin.web.exception.DuplicateResourceException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMarketplaceConfig.class)
class MarketplaceConnectionServiceIntegrationTest {

    @Autowired
    private MarketplaceConnectionService connectionService;

    @Autowired
    private MarketplaceConnectionRepository connectionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;

    @BeforeEach
    void setUp() {
        TestMarketplaceConfig.resetFlags();

        Tenant tenantA = Tenant.builder()
                .name("Marketplace Tenant A")
                .slug("mkt-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Marketplace Tenant B")
                .slug("mkt-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createConnection_shouldAuthenticateAndPersist() {
        var request = new CreateMarketplaceConnectionRequest(
                "CJ_DROPSHIPPING", "test-api-key", "CN", "standard");

        MarketplaceConnectionDto dto = connectionService.createConnection(tenantAId, request);

        assertNotNull(dto.id());
        assertEquals("CJ_DROPSHIPPING", dto.provider());
        assertEquals("ACTIVE", dto.status());
        assertTrue(dto.syncEnabled());
        assertEquals("CN", dto.defaultWarehouseId());
        assertNotNull(dto.lastConnectedAt());
    }

    @Test
    void createConnection_duplicate_shouldFail() {
        connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key1", null, null));

        assertThrows(DuplicateResourceException.class, () ->
                connectionService.createConnection(tenantAId,
                        new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key2", null, null)));
    }

    @Test
    void createConnection_authFailure_shouldThrow() {
        TestMarketplaceConfig.FAIL_AUTH.set(true);

        assertThrows(CjAuthException.class, () ->
                connectionService.createConnection(tenantAId,
                        new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "bad-key", null, null)));
    }

    @Test
    void getConnections_shouldReturnOnlyTenantConnections() {
        connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key-a", null, null));
        connectionService.createConnection(tenantBId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key-b", null, null));

        List<MarketplaceConnectionDto> connectionsA = connectionService.getConnections(tenantAId);
        assertEquals(1, connectionsA.size());

        List<MarketplaceConnectionDto> connectionsB = connectionService.getConnections(tenantBId);
        assertEquals(1, connectionsB.size());

        assertNotEquals(connectionsA.get(0).id(), connectionsB.get(0).id());
    }

    @Test
    void updateConnection_shouldUpdateFields() {
        MarketplaceConnectionDto created = connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key", null, null));

        MarketplaceConnectionDto updated = connectionService.updateConnection(tenantAId, created.id(),
                new UpdateMarketplaceConnectionRequest(null, false, "US", "express"));

        assertFalse(updated.syncEnabled());
        assertEquals("US", updated.defaultWarehouseId());
        assertEquals("express", updated.defaultShippingMethod());
    }

    @Test
    void deleteConnection_shouldRemove() {
        MarketplaceConnectionDto created = connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key", null, null));

        connectionService.deleteConnection(tenantAId, created.id());

        assertThrows(ResourceNotFoundException.class, () ->
                connectionService.getConnection(tenantAId, created.id()));
    }

    @Test
    void testConnection_success_shouldSetActive() {
        MarketplaceConnectionDto created = connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key", null, null));

        MarketplaceConnectionDto tested = connectionService.testConnection(tenantAId, created.id());
        assertEquals("ACTIVE", tested.status());
    }

    @Test
    void testConnection_authFailure_shouldSetTokenExpired() {
        MarketplaceConnectionDto created = connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key", null, null));

        TestMarketplaceConfig.FAIL_SEARCH.set(true);

        MarketplaceConnectionDto tested = connectionService.testConnection(tenantAId, created.id());
        assertEquals("TOKEN_EXPIRED", tested.status());
    }

    @Test
    void tenantIsolation_connectionFromTenantANotVisibleToTenantB() {
        MarketplaceConnectionDto created = connectionService.createConnection(tenantAId,
                new CreateMarketplaceConnectionRequest("CJ_DROPSHIPPING", "key", null, null));

        assertThrows(ResourceNotFoundException.class, () ->
                connectionService.getConnection(tenantBId, created.id()));
    }
}
