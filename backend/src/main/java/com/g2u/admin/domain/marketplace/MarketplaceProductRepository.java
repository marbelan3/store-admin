package com.g2u.admin.domain.marketplace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceProductRepository extends JpaRepository<MarketplaceProduct, UUID> {

    Page<MarketplaceProduct> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<MarketplaceProduct> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MarketplaceProduct> findByTenantIdAndExternalProductId(UUID tenantId, String externalProductId);

    List<MarketplaceProduct> findByConnectionIdAndExcludedFalse(UUID connectionId);

    @Query("SELECT mp FROM MarketplaceProduct mp WHERE mp.connection.id = :connectionId AND mp.excluded = false AND mp.syncStatus != 'DELISTED'")
    List<MarketplaceProduct> findSyncableByConnectionId(@Param("connectionId") UUID connectionId);

    Page<MarketplaceProduct> findByTenantIdAndExcluded(UUID tenantId, boolean excluded, Pageable pageable);

    @Query("SELECT mp FROM MarketplaceProduct mp WHERE mp.tenantId = :tenantId AND mp.marginAlertTriggered = true")
    List<MarketplaceProduct> findMarginAlerts(@Param("tenantId") UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndExcluded(UUID tenantId, boolean excluded);
}
