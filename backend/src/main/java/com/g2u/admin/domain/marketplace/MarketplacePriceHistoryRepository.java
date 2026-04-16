package com.g2u.admin.domain.marketplace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplacePriceHistoryRepository extends JpaRepository<MarketplacePriceHistory, UUID> {

    List<MarketplacePriceHistory> findByVariantMappingIdOrderByDetectedAtDesc(UUID variantMappingId);

    Page<MarketplacePriceHistory> findByVariantMappingId(UUID variantMappingId, Pageable pageable);
}
