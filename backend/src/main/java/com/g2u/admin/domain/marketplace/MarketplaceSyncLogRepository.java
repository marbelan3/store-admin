package com.g2u.admin.domain.marketplace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MarketplaceSyncLogRepository extends JpaRepository<MarketplaceSyncLog, UUID> {

    Page<MarketplaceSyncLog> findByConnectionIdOrderByStartedAtDesc(UUID connectionId, Pageable pageable);
}
