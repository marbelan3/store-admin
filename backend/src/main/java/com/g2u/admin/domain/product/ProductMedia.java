package com.g2u.admin.domain.product;

import com.g2u.admin.domain.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMedia extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "media_type", length = 20)
    @Builder.Default
    private String mediaType = "image";

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @Column(name = "media_id")
    private UUID mediaId;
}
