package com.g2u.admin.domain.marketplace;

import com.g2u.admin.domain.common.TenantAwareEntity;
import com.g2u.admin.domain.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceProduct extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private MarketplaceConnection connection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "external_product_id", nullable = false, length = 100)
    private String externalProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_rule", nullable = false, length = 20)
    @Builder.Default
    private PricingRule pricingRule = PricingRule.MARGIN;

    @Column(name = "target_margin_pct", precision = 5, scale = 2)
    private BigDecimal targetMarginPct;

    @Column(name = "fixed_markup_amount", precision = 12, scale = 2)
    private BigDecimal fixedMarkupAmount;

    @Column(name = "min_margin_pct", precision = 5, scale = 2)
    private BigDecimal minMarginPct;

    @Column(name = "current_margin_pct", precision = 5, scale = 2)
    private BigDecimal currentMarginPct;

    @Column(name = "margin_alert_triggered", nullable = false)
    @Builder.Default
    private boolean marginAlertTriggered = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean excluded = false;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @Column(name = "stock_alert_sent", nullable = false)
    @Builder.Default
    private boolean stockAlertSent = false;

    @OneToMany(mappedBy = "marketplaceProduct", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<MarketplaceVariantMapping> variantMappings = new ArrayList<>();
}
