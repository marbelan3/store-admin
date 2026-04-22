package com.g2u.admin.domain.marketplace;

import com.g2u.admin.domain.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "marketplace_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceConnection extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MarketplaceProvider provider;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "default_warehouse_id", length = 100)
    private String defaultWarehouseId;

    @Column(name = "default_shipping_method", length = 100)
    private String defaultShippingMethod;

    @Column(name = "sync_enabled", nullable = false)
    @Builder.Default
    private boolean syncEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.ACTIVE;

    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;
}
