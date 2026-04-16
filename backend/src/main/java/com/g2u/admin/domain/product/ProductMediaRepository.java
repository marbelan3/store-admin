package com.g2u.admin.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);

    void deleteByProductId(UUID productId);

    long countByProductId(UUID productId);

    Optional<ProductMedia> findByProductIdAndMediaId(UUID productId, UUID mediaId);

    @Query("SELECT pm FROM ProductMedia pm WHERE pm.product.id = :productId AND pm.product.tenantId = :tenantId ORDER BY pm.sortOrder ASC")
    List<ProductMedia> findByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
}
