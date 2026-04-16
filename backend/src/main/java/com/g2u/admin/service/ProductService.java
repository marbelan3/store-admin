package com.g2u.admin.service;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductMedia;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.domain.product.ProductSpecification;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.domain.tag.Tag;
import com.g2u.admin.domain.tag.TagRepository;
import com.g2u.admin.web.dto.BulkProductActionRequest;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.ProductDto;
import com.g2u.admin.web.dto.ProductListDto;
import com.g2u.admin.web.dto.UpdateProductRequest;
import com.g2u.admin.util.SlugUtils;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import com.g2u.admin.web.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;
    private final AuditService auditService;
    private final WebhookService webhookService;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          TagRepository tagRepository,
                          ProductMapper productMapper,
                          AuditService auditService,
                          WebhookService webhookService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.productMapper = productMapper;
        this.auditService = auditService;
        this.webhookService = webhookService;
    }

    @Transactional(readOnly = true)
    public Page<ProductListDto> getProducts(UUID tenantId, ProductStatus status, Pageable pageable) {
        return getProducts(tenantId, null, status, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductListDto> getProducts(UUID tenantId,
                                            String search,
                                            ProductStatus status,
                                            UUID categoryId,
                                            BigDecimal priceMin,
                                            BigDecimal priceMax,
                                            ProductSource source,
                                            Pageable pageable) {
        boolean hasExtraFilters = search != null || categoryId != null
                || priceMin != null || priceMax != null || source != null;

        // Use optimized JPQL projection when no extra filters are present
        if (!hasExtraFilters) {
            if (status != null) {
                return productRepository.findProductListByTenantIdAndStatus(tenantId, status, pageable);
            }
            return productRepository.findProductListByTenantId(tenantId, pageable);
        }

        // Use Specification for combined filtering
        Specification<Product> spec = ProductSpecification.buildFilter(
                tenantId, search, status, categoryId, priceMin, priceMax, source);
        return productRepository.findAll(spec, pageable).map(this::toProductListDto);
    }

    private ProductListDto toProductListDto(Product product) {
        String primaryImageUrl = product.getMedia().stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse(null);

        return new ProductListDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getStatus(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getCurrency(),
                product.getSku(),
                product.isTrackInventory(),
                product.getQuantity(),
                product.getPublishedAt(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                primaryImageUrl,
                product.getSource()
        );
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID tenantId, UUID productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return productMapper.toDto(product);
    }

    public ProductDto createProduct(UUID tenantId, UUID userId, CreateProductRequest request) {
        String baseSlug = request.slug() != null ? request.slug() : SlugUtils.generateSlug(request.name());
        String slug = SlugUtils.ensureUnique(baseSlug,
                s -> productRepository.existsByTenantIdAndSlug(tenantId, s));
        ProductStatus status = request.status() != null
                ? ProductStatus.valueOf(request.status().toUpperCase())
                : ProductStatus.DRAFT;

        Product product = Product.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .shortDescription(request.shortDescription())
                .status(status)
                .price(request.price())
                .compareAtPrice(request.compareAtPrice())
                .currency(request.currency() != null ? request.currency() : "UAH")
                .sku(request.sku())
                .barcode(request.barcode())
                .trackInventory(request.trackInventory())
                .quantity(request.quantity() != null ? request.quantity() : 0)
                .weight(request.weight())
                .weightUnit(request.weightUnit() != null ? request.weightUnit() : "kg")
                .metaTitle(request.metaTitle())
                .metaDescription(request.metaDescription())
                .attributes(request.attributes())
                .variants(new ArrayList<>())
                .media(new ArrayList<>())
                .categories(new HashSet<>())
                .tags(new HashSet<>())
                .build();
        product.setTenantId(tenantId);

        if (status == ProductStatus.ACTIVE) {
            product.setPublishedAt(Instant.now());
        }

        if (request.variants() != null) {
            for (CreateProductRequest.VariantRequest vr : request.variants()) {
                ProductVariant variant = buildVariant(vr, tenantId, product);
                product.getVariants().add(variant);
            }
        }

        if (request.media() != null) {
            for (CreateProductRequest.MediaRequest mr : request.media()) {
                ProductMedia media = buildMedia(mr, tenantId, product);
                product.getMedia().add(media);
            }
        }

        if (request.categoryIds() != null) {
            product.setCategories(loadCategories(tenantId, request.categoryIds()));
        }

        if (request.tagIds() != null) {
            product.setTags(loadTags(tenantId, request.tagIds()));
        }

        product = productRepository.save(product);
        log.info("Created product '{}' (id={}) for tenant {}", product.getName(), product.getId(), tenantId);

        auditService.log(tenantId, userId, "CREATE", "PRODUCT", product.getId(),
                Map.of("name", product.getName(), "status", product.getStatus().name()));

        webhookService.dispatch(tenantId, "product.created",
                Map.of("id", product.getId(), "name", product.getName(), "status", product.getStatus().name()));

        return productMapper.toDto(product);
    }

    public ProductDto updateProduct(UUID tenantId, UUID userId, UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (request.name() != null) product.setName(request.name());
        if (request.slug() != null) product.setSlug(request.slug());
        if (request.description() != null) product.setDescription(request.description());
        if (request.shortDescription() != null) product.setShortDescription(request.shortDescription());
        if (request.price() != null) product.setPrice(request.price());
        if (request.compareAtPrice() != null) product.setCompareAtPrice(request.compareAtPrice());
        if (request.currency() != null) product.setCurrency(request.currency());
        if (request.sku() != null) product.setSku(request.sku());
        if (request.barcode() != null) product.setBarcode(request.barcode());
        if (request.trackInventory() != null) product.setTrackInventory(request.trackInventory());
        if (request.quantity() != null) product.setQuantity(request.quantity());
        if (request.weight() != null) product.setWeight(request.weight());
        if (request.weightUnit() != null) product.setWeightUnit(request.weightUnit());
        if (request.metaTitle() != null) product.setMetaTitle(request.metaTitle());
        if (request.metaDescription() != null) product.setMetaDescription(request.metaDescription());
        if (request.attributes() != null) product.setAttributes(request.attributes());

        if (request.status() != null) {
            ProductStatus newStatus = ProductStatus.valueOf(request.status().toUpperCase());
            if (newStatus == ProductStatus.ACTIVE && product.getPublishedAt() == null) {
                product.setPublishedAt(Instant.now());
            }
            product.setStatus(newStatus);
        }

        if (request.variants() != null) {
            product.getVariants().clear();
            for (CreateProductRequest.VariantRequest vr : request.variants()) {
                product.getVariants().add(buildVariant(vr, tenantId, product));
            }
        }

        if (request.media() != null) {
            product.getMedia().clear();
            for (CreateProductRequest.MediaRequest mr : request.media()) {
                product.getMedia().add(buildMedia(mr, tenantId, product));
            }
        }

        if (request.categoryIds() != null) {
            product.setCategories(loadCategories(tenantId, request.categoryIds()));
        }

        if (request.tagIds() != null) {
            product.setTags(loadTags(tenantId, request.tagIds()));
        }

        product = productRepository.save(product);
        log.info("Updated product '{}' (id={}) for tenant {}", product.getName(), productId, tenantId);

        Map<String, Object> changes = new HashMap<>();
        if (request.name() != null) changes.put("name", request.name());
        if (request.status() != null) changes.put("status", request.status());
        if (request.price() != null) changes.put("price", request.price());
        auditService.log(tenantId, userId, "UPDATE", "PRODUCT", product.getId(), changes);

        webhookService.dispatch(tenantId, "product.updated",
                Map.of("id", product.getId(), "name", product.getName(), "status", product.getStatus().name()));

        return productMapper.toDto(product);
    }

    public int bulkAction(UUID tenantId, BulkProductActionRequest request) {
        List<Product> products = productRepository.findByTenantIdAndIdIn(tenantId, request.productIds());

        switch (request.action()) {
            case DELETE -> {
                productRepository.deleteAll(products);
                return products.size();
            }
            case CHANGE_STATUS -> {
                ProductStatus newStatus = ProductStatus.valueOf(request.status().toUpperCase());
                for (Product p : products) {
                    p.setStatus(newStatus);
                    if (newStatus == ProductStatus.ACTIVE && p.getPublishedAt() == null) {
                        p.setPublishedAt(Instant.now());
                    }
                }
                productRepository.saveAll(products);
                return products.size();
            }
            default -> throw new IllegalArgumentException("Unknown action: " + request.action());
        }
    }

    public void deleteProduct(UUID tenantId, UUID userId, UUID productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        productRepository.delete(product);
        log.info("Deleted product '{}' (id={}) from tenant {}", product.getName(), productId, tenantId);

        auditService.log(tenantId, userId, "DELETE", "PRODUCT", productId,
                Map.of("name", product.getName()));

        webhookService.dispatch(tenantId, "product.deleted",
                Map.of("id", productId, "name", product.getName()));
    }

    private ProductVariant buildVariant(CreateProductRequest.VariantRequest vr, UUID tenantId, Product product) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(vr.name())
                .sku(vr.sku())
                .price(vr.price())
                .compareAtPrice(vr.compareAtPrice())
                .costPrice(vr.costPrice())
                .quantity(vr.quantity() != null ? vr.quantity() : 0)
                .lowStockThreshold(vr.lowStockThreshold())
                .options(vr.options())
                .sortOrder(vr.sortOrder() != null ? vr.sortOrder() : 0)
                .active(vr.active())
                .build();
        variant.setTenantId(tenantId);
        return variant;
    }

    private ProductMedia buildMedia(CreateProductRequest.MediaRequest mr, UUID tenantId, Product product) {
        ProductMedia media = ProductMedia.builder()
                .product(product)
                .url(mr.url())
                .altText(mr.altText())
                .mediaType(mr.mediaType() != null ? mr.mediaType() : "image")
                .sortOrder(mr.sortOrder() != null ? mr.sortOrder() : 0)
                .primary(mr.primary())
                .build();
        media.setTenantId(tenantId);
        return media;
    }

    private Set<Category> loadCategories(UUID tenantId, Collection<UUID> ids) {
        return new HashSet<>(categoryRepository.findByTenantIdAndIdIn(tenantId, ids));
    }

    private Set<Tag> loadTags(UUID tenantId, Collection<UUID> ids) {
        return new HashSet<>(tagRepository.findByTenantIdAndIdIn(tenantId, ids));
    }

}
