package com.g2u.admin.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    long countByTenantIdAndReadFalse(UUID tenantId);

    Page<Notification> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.tenantId = :tenantId AND n.read = false")
    int markAllAsRead(@Param("tenantId") UUID tenantId);
}
