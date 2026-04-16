package com.g2u.admin.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntityType(UUID tenantId, String entityType, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.tenantId = :tenantId
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:dateFrom IS NULL OR a.createdAt >= :dateFrom)
              AND (:dateTo IS NULL OR a.createdAt <= :dateTo)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("userId") UUID userId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);
}
