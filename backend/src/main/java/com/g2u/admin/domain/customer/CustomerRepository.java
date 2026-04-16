package com.g2u.admin.domain.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Customer> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.tenantId = :tenantId
              AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Customer> search(@Param("tenantId") UUID tenantId,
                          @Param("search") String search,
                          Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
