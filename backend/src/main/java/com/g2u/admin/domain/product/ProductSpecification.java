package com.g2u.admin.domain.product;

import com.g2u.admin.domain.category.Category;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification builder for filtering products.
 * All filters are optional and combinable. tenantId is always required.
 */
public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> buildFilter(UUID tenantId,
                                                     String search,
                                                     ProductStatus status,
                                                     UUID categoryId,
                                                     BigDecimal priceMin,
                                                     BigDecimal priceMax) {
        return buildFilter(tenantId, search, status, categoryId, priceMin, priceMax, null);
    }

    public static Specification<Product> buildFilter(UUID tenantId,
                                                     String search,
                                                     ProductStatus status,
                                                     UUID categoryId,
                                                     BigDecimal priceMin,
                                                     BigDecimal priceMax,
                                                     ProductSource source) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // tenantId is always required
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            // text search: case-insensitive LIKE on name, sku, description
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, skuLike, descriptionLike));
            }

            // status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // source filter (OWN / MARKETPLACE)
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            // category filter via join on ManyToMany categories
            if (categoryId != null) {
                Join<Product, Category> categoriesJoin = root.join("categories");
                predicates.add(cb.equal(categoriesJoin.get("id"), categoryId));
                // ensure distinct results due to join
                query.distinct(true);
            }

            // price range filters
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
