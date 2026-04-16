package com.g2u.admin.domain.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Media> findByTenantId(UUID tenantId);

    List<Media> findByIdInAndTenantId(List<UUID> ids, UUID tenantId);
}
