package com.g2u.admin.domain.marketplace;

import com.g2u.admin.domain.common.TenantAwareEntity;
import com.g2u.admin.domain.product.ProductVariant;
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
@Table(name = "marketplace_variant_mappings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceVariantMapping extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketplace_product_id", nullable = false)
    private MarketplaceProduct marketplaceProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "cj_variant_id", length = 100)
    private String cjVariantId;

    @Column(name = "cj_sku", length = 100)
    private String cjSku;

    @Column(name = "previous_cj_sku", length = 100)
    private String previousCjSku;

    @Column(name = "fallback_cj_sku", length = 100)
    private String fallbackCjSku;

    @Column(name = "warehouse_id", length = 100)
    private String warehouseId;

    @Column(name = "warehouse_country", length = 5)
    private String warehouseCountry;

    @Column(name = "source_price", precision = 12, scale = 2)
    private BigDecimal sourcePrice;

    @Column(name = "shipping_estimate", precision = 12, scale = 2)
    private BigDecimal shippingEstimate;

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "stock_last_checked_at")
    private Instant stockLastCheckedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sku_status", nullable = false, length = 20)
    @Builder.Default
    private SkuStatus skuStatus = SkuStatus.ACTIVE;
}
