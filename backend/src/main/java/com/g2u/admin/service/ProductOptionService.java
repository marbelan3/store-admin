package com.g2u.admin.service;

import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductOption;
import com.g2u.admin.domain.product.ProductOptionRepository;
import com.g2u.admin.domain.product.ProductOptionValue;
import com.g2u.admin.domain.product.ProductOptionValueRepository;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.domain.product.ProductVariantRepository;
import com.g2u.admin.web.dto.CreateProductOptionRequest;
import com.g2u.admin.web.dto.CreateProductVariantRequest;
import com.g2u.admin.web.dto.ProductOptionDto;
import com.g2u.admin.web.dto.ProductVariantDetailDto;
import com.g2u.admin.web.dto.UpdateProductVariantRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import com.g2u.admin.web.mapper.ProductOptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProductOptionService {

    private static final Logger log = LoggerFactory.getLogger(ProductOptionService.class);

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductOptionMapper mapper;

    public ProductOptionService(ProductRepository productRepository,
                                ProductOptionRepository productOptionRepository,
                                ProductOptionValueRepository productOptionValueRepository,
                                ProductVariantRepository productVariantRepository,
                                ProductOptionMapper mapper) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.productOptionValueRepository = productOptionValueRepository;
        this.productVariantRepository = productVariantRepository;
        this.mapper = mapper;
    }

    // ---- Options ----

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getOptions(UUID productId, UUID tenantId) {
        verifyProduct(productId, tenantId);
        List<ProductOption> options = productOptionRepository
                .findByProductIdAndTenantIdOrderByDisplayOrderAsc(productId, tenantId);
        return mapper.toOptionDtoList(options);
    }

    public ProductOptionDto createOption(UUID productId, UUID tenantId, CreateProductOptionRequest request) {
        Product product = verifyProduct(productId, tenantId);

        ProductOption option = ProductOption.builder()
                .tenantId(tenantId)
                .product(product)
                .name(request.name())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .values(new ArrayList<>())
                .build();

        int order = 0;
        for (String val : request.values()) {
            ProductOptionValue optionValue = ProductOptionValue.builder()
                    .option(option)
                    .value(val)
                    .displayOrder(order++)
                    .build();
            option.getValues().add(optionValue);
        }

        option = productOptionRepository.save(option);
        log.info("Created product option '{}' (id={}) for product {} in tenant {}",
                option.getName(), option.getId(), productId, tenantId);
        return mapper.toOptionDto(option);
    }

    public void deleteOption(UUID optionId, UUID tenantId) {
        ProductOption option = productOptionRepository.findByIdAndTenantId(optionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductOption", optionId));
        productOptionRepository.delete(option);
        log.info("Deleted product option '{}' (id={}) from tenant {}", option.getName(), optionId, tenantId);
    }

    // ---- Variants ----

    @Transactional(readOnly = true)
    public List<ProductVariantDetailDto> getVariants(UUID productId, UUID tenantId) {
        verifyProduct(productId, tenantId);
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndTenantId(productId, tenantId);
        return mapper.toVariantDetailDtoList(variants);
    }

    public ProductVariantDetailDto createVariant(UUID productId, UUID tenantId, CreateProductVariantRequest request) {
        Product product = verifyProduct(productId, tenantId);

        // Validate SKU uniqueness within tenant
        if (request.sku() != null && !request.sku().isBlank()) {
            if (productVariantRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
                throw new IllegalArgumentException("SKU '" + request.sku() + "' already exists within this tenant");
            }
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(request.name())
                .sku(request.sku())
                .price(request.price())
                .compareAtPrice(request.compareAtPrice())
                .costPrice(request.costPrice())
                .quantity(request.quantity() != null ? request.quantity() : 0)
                .lowStockThreshold(request.lowStockThreshold())
                .options(new HashMap<>())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .active(request.active() != null ? request.active() : true)
                .optionValues(new HashSet<>())
                .build();
        variant.setTenantId(tenantId);

        if (request.optionValueIds() != null && !request.optionValueIds().isEmpty()) {
            Set<ProductOptionValue> optionValues = new HashSet<>(
                    productOptionValueRepository.findByIdIn(request.optionValueIds()));
            variant.setOptionValues(optionValues);
        }

        variant = productVariantRepository.save(variant);
        log.info("Created product variant '{}' (id={}) for product {} in tenant {}",
                variant.getName(), variant.getId(), productId, tenantId);
        return mapper.toVariantDetailDto(variant);
    }

    public ProductVariantDetailDto updateVariant(UUID variantId, UUID tenantId, UpdateProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));

        if (request.name() != null) variant.setName(request.name());
        if (request.sku() != null) {
            if (!request.sku().isBlank()
                    && productVariantRepository.existsBySkuAndTenantIdAndIdNot(request.sku(), tenantId, variantId)) {
                throw new IllegalArgumentException("SKU '" + request.sku() + "' already exists within this tenant");
            }
            variant.setSku(request.sku());
        }
        if (request.price() != null) variant.setPrice(request.price());
        if (request.compareAtPrice() != null) variant.setCompareAtPrice(request.compareAtPrice());
        if (request.costPrice() != null) variant.setCostPrice(request.costPrice());
        if (request.quantity() != null) variant.setQuantity(request.quantity());
        if (request.lowStockThreshold() != null) variant.setLowStockThreshold(request.lowStockThreshold());
        if (request.sortOrder() != null) variant.setSortOrder(request.sortOrder());
        if (request.active() != null) variant.setActive(request.active());

        if (request.optionValueIds() != null) {
            Set<ProductOptionValue> optionValues = new HashSet<>(
                    productOptionValueRepository.findByIdIn(request.optionValueIds()));
            variant.setOptionValues(optionValues);
        }

        variant = productVariantRepository.save(variant);
        log.info("Updated product variant '{}' (id={}) in tenant {}", variant.getName(), variantId, tenantId);
        return mapper.toVariantDetailDto(variant);
    }

    public void deleteVariant(UUID variantId, UUID tenantId) {
        ProductVariant variant = productVariantRepository.findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        productVariantRepository.delete(variant);
        log.info("Deleted product variant '{}' (id={}) from tenant {}", variant.getName(), variantId, tenantId);
    }

    private Product verifyProduct(UUID productId, UUID tenantId) {
        return productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }
}
