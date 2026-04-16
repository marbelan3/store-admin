package com.g2u.admin.domain.marketplace;

import com.g2u.admin.domain.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "marketplace_watchlist_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceWatchlistItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private MarketplaceConnection connection;

    @Column(name = "external_product_id", nullable = false, length = 100)
    private String externalProductId;

    @Column(name = "external_product_name", length = 500)
    private String externalProductName;

    @Column(name = "external_image_url", length = 1000)
    private String externalImageUrl;

    @Column(name = "external_price", precision = 12, scale = 2)
    private BigDecimal externalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_stock_status", length = 20)
    @Builder.Default
    private ExternalStockStatus externalStockStatus = ExternalStockStatus.IN_STOCK;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @jakarta.persistence.PrePersist
    protected void onCreateWatchlist() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
