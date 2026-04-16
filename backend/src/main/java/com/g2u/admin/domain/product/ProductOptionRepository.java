package com.g2u.admin.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {

    List<ProductOption> findByProductIdAndTenantIdOrderByDisplayOrderAsc(UUID productId, UUID tenantId);

    Optional<ProductOption> findByIdAndTenantId(UUID id, UUID tenantId);

    void deleteByProductIdAndTenantId(UUID productId, UUID tenantId);
}
