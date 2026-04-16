package com.g2u.admin.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderBySortOrderAsc(UUID productId);

    List<ProductVariant> findByProductIdAndTenantId(UUID productId, UUID tenantId);

    Optional<ProductVariant> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsBySkuAndTenantId(String sku, UUID tenantId);

    boolean existsBySkuAndTenantIdAndIdNot(String sku, UUID tenantId, UUID id);

    void deleteByProductId(UUID productId);

    void deleteByProductIdAndTenantId(UUID productId, UUID tenantId);

    List<ProductVariant> findByTenantId(UUID tenantId);

    List<ProductVariant> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
}
