package com.g2u.admin.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.cjOrderId IS NOT NULL AND o.status IN :statuses")
    java.util.List<Order> findByCjOrderIdNotNullAndStatusIn(@Param("statuses") java.util.Collection<OrderStatus> statuses);

    Optional<Order> findByCjOrderId(String cjOrderId);
}
