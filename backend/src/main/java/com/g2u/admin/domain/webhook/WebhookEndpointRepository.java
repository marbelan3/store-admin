package com.g2u.admin.domain.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    Page<WebhookEndpoint> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Optional<WebhookEndpoint> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WebhookEndpoint> findByTenantIdAndActiveTrue(UUID tenantId);
}
