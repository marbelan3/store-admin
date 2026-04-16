package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.ConnectionStatus;
import com.g2u.admin.domain.marketplace.MarketplaceConnection;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import com.g2u.admin.domain.marketplace.MarketplaceProvider;
import com.g2u.admin.infrastructure.marketplace.CjAuthException;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.web.dto.CreateMarketplaceConnectionRequest;
import com.g2u.admin.web.dto.MarketplaceConnectionDto;
import com.g2u.admin.web.dto.UpdateMarketplaceConnectionRequest;
import com.g2u.admin.web.exception.DuplicateResourceException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceConnectionService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceConnectionService.class);
    private static final long TOKEN_TTL_DAYS = 15;

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceAdapter marketplaceAdapter;

    public MarketplaceConnectionService(MarketplaceConnectionRepository connectionRepository,
                                         MarketplaceAdapter marketplaceAdapter) {
        this.connectionRepository = connectionRepository;
        this.marketplaceAdapter = marketplaceAdapter;
    }

    @Transactional(readOnly = true)
    public List<MarketplaceConnectionDto> getConnections(UUID tenantId) {
        return connectionRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceConnectionDto getConnection(UUID tenantId, UUID id) {
        return toDto(findConnection(tenantId, id));
    }

    public MarketplaceConnectionDto createConnection(UUID tenantId, CreateMarketplaceConnectionRequest request) {
        MarketplaceProvider provider = MarketplaceProvider.valueOf(request.provider());

        connectionRepository.findByTenantIdAndProvider(tenantId, provider)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Connection for " + provider + " already exists");
                });

        // Authenticate to validate API key and get token
        String accessToken = marketplaceAdapter.authenticate(request.email(), request.apiKey());

        MarketplaceConnection connection = MarketplaceConnection.builder()
                .provider(provider)
                .email(request.email())
                .apiKey(request.apiKey())
                .accessToken(accessToken)
                .tokenExpiresAt(Instant.now().plus(TOKEN_TTL_DAYS, ChronoUnit.DAYS))
                .defaultWarehouseId(request.defaultWarehouseId())
                .defaultShippingMethod(request.defaultShippingMethod())
                .syncEnabled(true)
                .status(ConnectionStatus.ACTIVE)
                .lastConnectedAt(Instant.now())
                .build();
        connection.setTenantId(tenantId);

        return toDto(connectionRepository.save(connection));
    }

    public MarketplaceConnectionDto updateConnection(UUID tenantId, UUID id, UpdateMarketplaceConnectionRequest request) {
        MarketplaceConnection connection = findConnection(tenantId, id);

        if (request.email() != null && !request.email().isBlank()) {
            connection.setEmail(request.email());
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            // Re-authenticate with new API key
            String email = connection.getEmail();
            String accessToken = marketplaceAdapter.authenticate(email, request.apiKey());
            connection.setApiKey(request.apiKey());
            connection.setAccessToken(accessToken);
            connection.setTokenExpiresAt(Instant.now().plus(TOKEN_TTL_DAYS, ChronoUnit.DAYS));
            connection.setStatus(ConnectionStatus.ACTIVE);
            connection.setLastConnectedAt(Instant.now());
        }
        if (request.syncEnabled() != null) {
            connection.setSyncEnabled(request.syncEnabled());
        }
        if (request.defaultWarehouseId() != null) {
            connection.setDefaultWarehouseId(request.defaultWarehouseId());
        }
        if (request.defaultShippingMethod() != null) {
            connection.setDefaultShippingMethod(request.defaultShippingMethod());
        }

        return toDto(connectionRepository.save(connection));
    }

    public void deleteConnection(UUID tenantId, UUID id) {
        MarketplaceConnection connection = findConnection(tenantId, id);
        connectionRepository.delete(connection);
    }

    public MarketplaceConnectionDto testConnection(UUID tenantId, UUID id) {
        MarketplaceConnection connection = findConnection(tenantId, id);
        try {
            String accessToken = refreshTokenIfNeeded(connection);
            // Try a lightweight API call to verify connectivity
            marketplaceAdapter.searchProducts(accessToken, "test", 1, 1);
            connection.setStatus(ConnectionStatus.ACTIVE);
            connection.setLastConnectedAt(Instant.now());
        } catch (CjAuthException e) {
            connection.setStatus(ConnectionStatus.TOKEN_EXPIRED);
            log.warn("Connection test failed for tenant={}, connection={}: {}", tenantId, id, e.getMessage());
        } catch (Exception e) {
            connection.setStatus(ConnectionStatus.ERROR);
            log.warn("Connection test failed for tenant={}, connection={}: {}", tenantId, id, e.getMessage());
        }
        return toDto(connectionRepository.save(connection));
    }

    /**
     * Get a valid access token, refreshing if expired.
     */
    public String getValidAccessToken(MarketplaceConnection connection) {
        return refreshTokenIfNeeded(connection);
    }

    private String refreshTokenIfNeeded(MarketplaceConnection connection) {
        if (connection.getTokenExpiresAt() != null
                && connection.getTokenExpiresAt().isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            // Token still valid for more than 1 day
            return connection.getAccessToken();
        }
        // Refresh token
        log.info("Refreshing CJ access token for connection {}", connection.getId());
        String newToken = marketplaceAdapter.authenticate(connection.getEmail(), connection.getApiKey());
        connection.setAccessToken(newToken);
        connection.setTokenExpiresAt(Instant.now().plus(TOKEN_TTL_DAYS, ChronoUnit.DAYS));
        connection.setStatus(ConnectionStatus.ACTIVE);
        connection.setLastConnectedAt(Instant.now());
        connectionRepository.save(connection);
        return newToken;
    }

    private MarketplaceConnection findConnection(UUID tenantId, UUID id) {
        return connectionRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceConnection", id));
    }

    private MarketplaceConnectionDto toDto(MarketplaceConnection c) {
        return new MarketplaceConnectionDto(
                c.getId(),
                c.getProvider().name(),
                c.getEmail(),
                c.getStatus().name(),
                c.isSyncEnabled(),
                c.getDefaultWarehouseId(),
                c.getDefaultShippingMethod(),
                c.getLastConnectedAt(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
