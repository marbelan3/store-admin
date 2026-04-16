package com.g2u.admin.domain.marketplace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceWatchlistItemRepository extends JpaRepository<MarketplaceWatchlistItem, UUID> {

    Page<MarketplaceWatchlistItem> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<MarketplaceWatchlistItem> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MarketplaceWatchlistItem> findByTenantIdAndExternalProductId(UUID tenantId, String externalProductId);

    boolean existsByTenantIdAndExternalProductId(UUID tenantId, String externalProductId);

    long countByTenantId(UUID tenantId);
}
