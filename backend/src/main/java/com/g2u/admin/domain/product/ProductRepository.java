package com.g2u.admin.domain.product;

import com.g2u.admin.web.dto.ProductListDto;
import com.g2u.admin.web.dto.RecentProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);

    Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
            SELECT new com.g2u.admin.web.dto.ProductListDto(
                p.id, p.name, p.slug, p.status, p.price, p.compareAtPrice,
                p.currency, p.sku, p.trackInventory, p.quantity,
                p.publishedAt, p.createdAt, p.updatedAt,
                (SELECT m.url FROM ProductMedia m WHERE m.product = p AND m.primary = true),
                p.source
            )
            FROM Product p
            WHERE p.tenantId = :tenantId
            """)
    Page<ProductListDto> findProductListByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("""
            SELECT new com.g2u.admin.web.dto.ProductListDto(
                p.id, p.name, p.slug, p.status, p.price, p.compareAtPrice,
                p.currency, p.sku, p.trackInventory, p.quantity,
                p.publishedAt, p.createdAt, p.updatedAt,
                (SELECT m.url FROM ProductMedia m WHERE m.product = p AND m.primary = true),
                p.source
            )
            FROM Product p
            WHERE p.tenantId = :tenantId AND p.status = :status
            """)
    Page<ProductListDto> findProductListByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId, @Param("status") ProductStatus status, Pageable pageable);

    Optional<Product> findByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    Optional<Product> findByTenantIdAndSku(UUID tenantId, String sku);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") ProductStatus status);

    List<Product> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.trackInventory = true
              AND (p.quantity = 0 OR p.quantity <= p.lowStockThreshold)
            """)
    long countLowStock(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.id NOT IN (SELECT DISTINCT m.product.id FROM ProductMedia m WHERE m.product.tenantId = :tenantId)
            """)
    long countWithNoMedia(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.categories IS EMPTY
            """)
    long countUncategorized(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT new com.g2u.admin.web.dto.RecentProductDto(
                p.id, p.name, p.updatedAt
            )
            FROM Product p
            WHERE p.tenantId = :tenantId
            ORDER BY p.updatedAt DESC
            """)
    List<RecentProductDto> findRecentlyUpdated(@Param("tenantId") UUID tenantId, Pageable pageable);
}
