package com.g2u.admin.domain.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceVariantMappingRepository extends JpaRepository<MarketplaceVariantMapping, UUID> {

    List<MarketplaceVariantMapping> findByMarketplaceProductId(UUID marketplaceProductId);

    List<MarketplaceVariantMapping> findByTenantId(UUID tenantId);

    Optional<MarketplaceVariantMapping> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MarketplaceVariantMapping> findByCjSku(String cjSku);

    @Query("SELECT m FROM MarketplaceVariantMapping m WHERE m.marketplaceProduct.connection.id = :connectionId AND m.skuStatus = :status")
    List<MarketplaceVariantMapping> findByConnectionIdAndSkuStatus(
            @Param("connectionId") UUID connectionId,
            @Param("status") SkuStatus status);

    @Query("SELECT m FROM MarketplaceVariantMapping m WHERE m.tenantId = :tenantId AND m.stockQuantity <= :threshold AND m.stockQuantity >= 0")
    List<MarketplaceVariantMapping> findLowStockByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("threshold") int threshold);
}
