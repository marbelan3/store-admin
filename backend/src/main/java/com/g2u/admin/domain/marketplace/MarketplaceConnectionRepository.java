package com.g2u.admin.domain.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceConnectionRepository extends JpaRepository<MarketplaceConnection, UUID> {

    List<MarketplaceConnection> findByTenantId(UUID tenantId);

    Optional<MarketplaceConnection> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MarketplaceConnection> findByTenantIdAndProvider(UUID tenantId, MarketplaceProvider provider);

    List<MarketplaceConnection> findBySyncEnabledTrueAndStatus(ConnectionStatus status);
}
