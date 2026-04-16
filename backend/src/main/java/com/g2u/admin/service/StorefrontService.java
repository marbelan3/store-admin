package com.g2u.admin.service;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductMedia;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.web.dto.storefront.StorefrontCategoryDto;
import com.g2u.admin.web.dto.storefront.StorefrontInventoryDto;
import com.g2u.admin.web.dto.storefront.StorefrontProductDetailDto;
import com.g2u.admin.web.dto.storefront.StorefrontProductDto;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StorefrontService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public StorefrontService(ProductRepository productRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<StorefrontProductDto> getProducts(UUID tenantId, String categorySlug, String tagSlug,
                                                   String search, Pageable pageable) {
        // Get active products for the tenant
        Page<Product> products = productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable);

        // Apply additional filters in-memory for now (can be optimized with custom queries later)
        List<Product> filtered = products.getContent().stream()
                .filter(p -> categorySlug == null || p.getCategories().stream()
                        .anyMatch(c -> c.getSlug().equals(categorySlug)))
                .filter(p -> tagSlug == null || p.getTags().stream()
                        .anyMatch(t -> t.getSlug().equals(tagSlug)))
                .filter(p -> search == null || search.isBlank() ||
                        p.getName().toLowerCase().contains(search.toLowerCase()) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(search.toLowerCase())))
                .toList();

        List<StorefrontProductDto> dtos = filtered.stream()
                .map(this::toProductDto)
                .toList();

        return new PageImpl<>(dtos, pageable, products.getTotalElements());
    }

    public StorefrontProductDetailDto getProductBySlug(UUID tenantId, String slug) {
        Product product = productRepository.findByTenantIdAndSlug(tenantId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product", slug);
        }

        return toProductDetailDto(product);
    }

    public List<StorefrontCategoryDto> getCategoryTree(UUID tenantId) {
        List<Category> roots = categoryRepository.findByTenantIdAndParentIsNullOrderBySortOrderAsc(tenantId);
        return roots.stream()
                .filter(Category::isActive)
                .map(this::toCategoryDto)
                .toList();
    }

    public StorefrontCategoryDto getCategoryBySlug(UUID tenantId, String slug) {
        Category category = categoryRepository.findByTenantIdAndSlug(tenantId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category", slug);
        }

        return toCategoryDto(category);
    }

    public Page<StorefrontProductDto> getProductsByCategory(UUID tenantId, String categorySlug, Pageable pageable) {
        Category category = categoryRepository.findByTenantIdAndSlug(tenantId, categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categorySlug));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category", categorySlug);
        }

        Page<Product> products = productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable);

        List<StorefrontProductDto> filtered = products.getContent().stream()
                .filter(p -> p.getCategories().stream()
                        .anyMatch(c -> c.getId().equals(category.getId())))
                .map(this::toProductDto)
                .toList();

        return new PageImpl<>(filtered, pageable, products.getTotalElements());
    }

    public StorefrontInventoryDto getInventory(UUID tenantId, String sku) {
        // Check product-level SKU first
        Product product = productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE,
                        Pageable.unpaged())
                .getContent().stream()
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElse(null);

        if (product != null) {
            return new StorefrontInventoryDto(sku, product.getQuantity() > 0, product.getQuantity());
        }

        // Check variant-level SKU
        for (Product p : productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE,
                Pageable.unpaged()).getContent()) {
            for (ProductVariant v : p.getVariants()) {
                if (sku.equals(v.getSku())) {
                    return new StorefrontInventoryDto(sku, v.getQuantity() > 0, v.getQuantity());
                }
            }
        }

        throw new ResourceNotFoundException("SKU", sku);
    }

    public List<StorefrontInventoryDto> getBulkInventory(UUID tenantId, List<String> skus) {
        List<StorefrontInventoryDto> results = new ArrayList<>();

        List<Product> activeProducts = productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE,
                Pageable.unpaged()).getContent();

        for (String sku : skus) {
            StorefrontInventoryDto found = null;

            // Check product-level SKU
            for (Product p : activeProducts) {
                if (sku.equals(p.getSku())) {
                    found = new StorefrontInventoryDto(sku, p.getQuantity() > 0, p.getQuantity());
                    break;
                }
            }

            // Check variant-level SKU if not found
            if (found == null) {
                outer:
                for (Product p : activeProducts) {
                    for (ProductVariant v : p.getVariants()) {
                        if (sku.equals(v.getSku())) {
                            found = new StorefrontInventoryDto(sku, v.getQuantity() > 0, v.getQuantity());
                            break outer;
                        }
                    }
                }
            }

            if (found != null) {
                results.add(found);
            } else {
                results.add(new StorefrontInventoryDto(sku, false, 0));
            }
        }

        return results;
    }

    private StorefrontProductDto toProductDto(Product product) {
        String primaryImageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse(product.getMedia().isEmpty() ? null : product.getMedia().get(0).getUrl());

        return new StorefrontProductDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getCurrency(),
                primaryImageUrl
        );
    }

    private StorefrontProductDetailDto toProductDetailDto(Product product) {
        List<StorefrontProductDetailDto.VariantDto> variants = product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(v -> new StorefrontProductDetailDto.VariantDto(
                        v.getId(),
                        v.getName(),
                        v.getSku(),
                        v.getPrice(),
                        v.getCompareAtPrice(),
                        v.getQuantity(),
                        v.getOptions(),
                        v.isActive()
                ))
                .toList();

        List<StorefrontProductDetailDto.MediaDto> media = product.getMedia().stream()
                .map(m -> new StorefrontProductDetailDto.MediaDto(
                        m.getId(),
                        m.getUrl(),
                        m.getAltText(),
                        m.getMediaType(),
                        m.getSortOrder(),
                        m.isPrimary()
                ))
                .toList();

        var categories = product.getCategories().stream()
                .map(c -> new StorefrontProductDetailDto.CategoryRefDto(c.getId(), c.getName(), c.getSlug()))
                .collect(Collectors.toSet());

        var tags = product.getTags().stream()
                .map(t -> new StorefrontProductDetailDto.TagRefDto(t.getId(), t.getName(), t.getSlug()))
                .collect(Collectors.toSet());

        return new StorefrontProductDetailDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getCurrency(),
                product.getSku(),
                product.getAttributes(),
                variants,
                media,
                categories,
                tags
        );
    }

    private StorefrontCategoryDto toCategoryDto(Category category) {
        List<StorefrontCategoryDto> children = category.getChildren().stream()
                .filter(Category::isActive)
                .map(this::toCategoryDto)
                .toList();

        return new StorefrontCategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getMetaTitle(),
                category.getMetaDescription(),
                children
        );
    }
}
