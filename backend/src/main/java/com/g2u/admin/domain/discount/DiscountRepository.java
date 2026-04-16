package com.g2u.admin.domain.discount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {

    Optional<Discount> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Discount> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Discount> findByCodeAndTenantId(String code, UUID tenantId);

    List<Discount> findByTenantIdAndActiveTrue(UUID tenantId);

    boolean existsByCodeAndTenantId(String code, UUID tenantId);
}
