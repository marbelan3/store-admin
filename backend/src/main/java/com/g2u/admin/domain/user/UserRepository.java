package com.g2u.admin.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOauthProviderAndOauthProviderId(String provider, String providerId);

    List<User> findByTenantId(UUID tenantId);

    boolean existsByEmail(String email);

    long countByTenantId(UUID tenantId);
}
