package com.g2u.admin.domain.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByTenantId(UUID tenantId);

    Optional<Tag> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Tag> findByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    List<Tag> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    long countByTenantId(UUID tenantId);
}
