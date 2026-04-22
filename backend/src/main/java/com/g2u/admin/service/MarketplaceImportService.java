package com.g2u.admin.service;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.domain.marketplace.MarketplaceConnection;
import com.g2u.admin.domain.marketplace.MarketplaceConnectionRepository;
import com.g2u.admin.domain.marketplace.MarketplaceProduct;
import com.g2u.admin.domain.marketplace.MarketplaceProductRepository;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMapping;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMappingRepository;
import com.g2u.admin.domain.marketplace.PricingRule;
import com.g2u.admin.domain.marketplace.SkuStatus;
import com.g2u.admin.domain.marketplace.SyncStatus;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductMedia;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductSource;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjProduct;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter.CjVariant;
import com.g2u.admin.web.dto.CjCatalogProductDto;
import com.g2u.admin.web.dto.CjCategoryDto;
import com.g2u.admin.web.dto.CjProductDetailDto;
import com.g2u.admin.web.dto.ImportProductRequest;
import com.g2u.admin.web.dto.MarketplaceProductDto;
import com.g2u.admin.web.exception.DuplicateResourceException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MarketplaceImportService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceImportService.class);

    private final MarketplaceConnectionRepository connectionRepository;
    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MarketplaceAdapter marketplaceAdapter;
    private final MarketplaceConnectionService connectionService;

    public MarketplaceImportService(MarketplaceConnectionRepository connectionRepository,
                                     MarketplaceProductRepository marketplaceProductRepository,
                                     MarketplaceVariantMappingRepository variantMappingRepository,
                                     ProductRepository productRepository,
                                     CategoryRepository categoryRepository,
                                     MarketplaceAdapter marketplaceAdapter,
                                     MarketplaceConnectionService connectionService) {
        this.connectionRepository = connectionRepository;
        this.marketplaceProductRepository = marketplaceProductRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.marketplaceAdapter = marketplaceAdapter;
        this.connectionService = connectionService;
    }

    // --- CJ Catalog Proxy ---

    @Transactional(readOnly = true)
    public List<CjCatalogProductDto> searchCatalog(UUID tenantId, UUID connectionId,
                                                    String query, int page, int pageSize) {
        MarketplaceConnection connection = findConnection(tenantId, connectionId);
        String token = connectionService.getValidAccessToken(connection);

        List<CjProduct> products = marketplaceAdapter.searchProducts(token, query, page, pageSize);
        return products.stream()
                .map(p -> new CjCatalogProductDto(p.pid(), p.productName(), p.productImage(),
                        p.categoryName(), p.sellPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CjCatalogProductDto> listByCategory(UUID tenantId, UUID connectionId,
                                                     String categoryId, int page, int pageSize) {
        MarketplaceConnection connection = findConnection(tenantId, connectionId);
        String token = connectionService.getValidAccessToken(connection);

        List<CjProduct> products = marketplaceAdapter.listProductsByCategory(token, categoryId, page, pageSize);
        return products.stream()
                .map(p -> new CjCatalogProductDto(p.pid(), p.productName(), p.productImage(),
                        p.categoryName(), p.sellPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CjCategoryDto> getCategories(UUID tenantId, UUID connectionId) {
        MarketplaceConnection connection = findConnection(tenantId, connectionId);
        String token = connectionService.getValidAccessToken(connection);

        return marketplaceAdapter.getCategories(token).stream()
                .map(c -> new CjCategoryDto(
                        c.name(),
                        c.subCategories().stream()
                                .map(s -> new CjCategoryDto.SubCategory(
                                        s.name(),
                                        s.categories().stream()
                                                .map(l -> new CjCategoryDto.LeafCategory(l.categoryId(), l.categoryName()))
                                                .toList()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CjProductDetailDto getProductDetail(UUID tenantId, UUID connectionId, String externalProductId) {
        MarketplaceConnection connection = findConnection(tenantId, connectionId);
        String token = connectionService.getValidAccessToken(connection);

        CjProduct product = marketplaceAdapter.getProductDetails(token, externalProductId);
        List<CjVariant> variants = product.variants();
        if (variants == null || variants.isEmpty()) {
            variants = marketplaceAdapter.getVariants(token, externalProductId);
        }

        List<CjProductDetailDto.CjVariantDto> variantDtos = new ArrayList<>();
        for (CjVariant v : variants) {
            int stock = v.stock();
            if (stock <= 0) {
                try {
                    stock = marketplaceAdapter.getStock(token, v.vid());
                } catch (Exception e) {
                    log.warn("Failed to fetch stock for variant {}: {}", v.vid(), e.getMessage());
                }
            }
            variantDtos.add(new CjProductDetailDto.CjVariantDto(
                    v.vid(), v.variantName(), v.variantSku(),
                    v.variantSellPrice(), v.variantImage(), stock));
        }

        return new CjProductDetailDto(
                product.pid(), product.productName(), product.description(),
                product.productImage(), product.categoryName(), product.sellPrice(),
                variantDtos);
    }

    // --- Import ---

    public MarketplaceProductDto importProduct(UUID tenantId, UUID userId, ImportProductRequest request) {
        // Check duplicate
        marketplaceProductRepository.findByTenantIdAndExternalProductId(tenantId, request.externalProductId())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Product already imported: " + request.externalProductId());
                });

        MarketplaceConnection connection = findConnection(tenantId, request.connectionId());
        String token = connectionService.getValidAccessToken(connection);

        // Validate product exists on CJ
        CjProduct cjProduct = marketplaceAdapter.getProductDetails(token, request.externalProductId());

        PricingRule pricingRule = request.pricingRule() != null
                ? PricingRule.valueOf(request.pricingRule())
                : PricingRule.MARGIN;

        BigDecimal defaultMargin = request.targetMarginPct() != null
                ? request.targetMarginPct()
                : new BigDecimal("30.00");

        // Create local Product
        String slug = generateSlug(cjProduct.productName(), tenantId);
        Product product = Product.builder()
                .name(cjProduct.productName())
                .slug(slug)
                .description(cjProduct.description())
                .status(ProductStatus.DRAFT)
                .source(ProductSource.MARKETPLACE)
                .price(calculateSellingPrice(cjProduct.sellPrice(), pricingRule, defaultMargin,
                        request.fixedMarkupAmount(), null))
                .currency("USD")
                .trackInventory(true)
                .quantity(0)
                .build();
        product.setTenantId(tenantId);

        // Add category if specified
        if (request.categoryId() != null) {
            categoryRepository.findById(request.categoryId())
                    .ifPresent(cat -> product.getCategories().add(cat));
        }

        // Add media from CJ product image
        if (cjProduct.productImage() != null && !cjProduct.productImage().isBlank()) {
            ProductMedia media = ProductMedia.builder()
                    .product(product)
                    .url(cjProduct.productImage())
                    .altText(cjProduct.productName())
                    .mediaType("image")
                    .sortOrder(0)
                    .primary(true)
                    .build();
            media.setTenantId(tenantId);
            product.getMedia().add(media);
        }

        // Create variants from selected CJ variants
        int totalStock = 0;
        int sortOrder = 0;
        Set<String> seenImages = new HashSet<>();
        if (cjProduct.productImage() != null) {
            seenImages.add(cjProduct.productImage());
        }

        for (ImportProductRequest.ImportVariantRequest vr : request.variants()) {
            // Find matching CJ variant for price info
            BigDecimal sourcePrice = cjProduct.sellPrice();
            String variantName = "Variant " + vr.cjSku();
            String variantImage = null;

            if (cjProduct.variants() != null) {
                for (CjVariant cv : cjProduct.variants()) {
                    if (cv.vid().equals(vr.cjVariantId())) {
                        sourcePrice = cv.variantSellPrice();
                        variantName = cv.variantName();
                        variantImage = cv.variantImage();
                        break;
                    }
                }
            }

            BigDecimal sellingPrice = calculateSellingPrice(sourcePrice, pricingRule,
                    defaultMargin, request.fixedMarkupAmount(), vr.manualPrice());

            int stock = 0;
            try {
                stock = marketplaceAdapter.getStock(token, vr.cjVariantId());
            } catch (Exception e) {
                log.warn("Failed to get stock for variant {}: {}", vr.cjVariantId(), e.getMessage());
            }
            totalStock += stock;

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .name(variantName)
                    .sku("CJ-" + vr.cjSku())
                    .price(sellingPrice)
                    .costPrice(sourcePrice)
                    .quantity(stock)
                    .sortOrder(sortOrder++)
                    .active(true)
                    .options(Map.of())
                    .build();
            variant.setTenantId(tenantId);
            product.getVariants().add(variant);

            // De-duplicate variant images
            if (variantImage != null && !variantImage.isBlank() && seenImages.add(variantImage)) {
                ProductMedia variantMedia = ProductMedia.builder()
                        .product(product)
                        .url(variantImage)
                        .altText(variantName)
                        .mediaType("image")
                        .sortOrder(product.getMedia().size())
                        .primary(false)
                        .build();
                variantMedia.setTenantId(tenantId);
                product.getMedia().add(variantMedia);
            }
        }

        product.setQuantity(totalStock);
        Product savedProduct = productRepository.save(product);

        // Create MarketplaceProduct
        MarketplaceProduct mp = MarketplaceProduct.builder()
                .connection(connection)
                .product(savedProduct)
                .externalProductId(request.externalProductId())
                .syncStatus(SyncStatus.SYNCED)
                .pricingRule(pricingRule)
                .targetMarginPct(defaultMargin)
                .fixedMarkupAmount(request.fixedMarkupAmount())
                .minMarginPct(request.minMarginPct() != null ? request.minMarginPct() : new BigDecimal("10.00"))
                .lowStockThreshold(request.lowStockThreshold())
                .build();
        mp.setTenantId(tenantId);
        MarketplaceProduct savedMp = marketplaceProductRepository.save(mp);

        // Create variant mappings
        List<MarketplaceVariantMapping> mappings = new ArrayList<>();
        for (int i = 0; i < request.variants().size(); i++) {
            ImportProductRequest.ImportVariantRequest vr = request.variants().get(i);
            ProductVariant savedVariant = savedProduct.getVariants().get(i);

            BigDecimal sourcePrice = cjProduct.sellPrice();
            if (cjProduct.variants() != null) {
                for (CjVariant cv : cjProduct.variants()) {
                    if (cv.vid().equals(vr.cjVariantId())) {
                        sourcePrice = cv.variantSellPrice();
                        break;
                    }
                }
            }

            MarketplaceVariantMapping mapping = MarketplaceVariantMapping.builder()
                    .marketplaceProduct(savedMp)
                    .variant(savedVariant)
                    .cjVariantId(vr.cjVariantId())
                    .cjSku(vr.cjSku())
                    .warehouseId(vr.warehouseId() != null ? vr.warehouseId() : connection.getDefaultWarehouseId())
                    .warehouseCountry(vr.warehouseCountry())
                    .sourcePrice(sourcePrice)
                    .stockQuantity(savedVariant.getQuantity())
                    .stockLastCheckedAt(Instant.now())
                    .skuStatus(SkuStatus.ACTIVE)
                    .build();
            mapping.setTenantId(tenantId);
            mappings.add(mapping);
        }
        variantMappingRepository.saveAll(mappings);

        // Calculate initial margin
        updateMarginCalculation(savedMp, mappings);
        marketplaceProductRepository.save(savedMp);

        log.info("Imported marketplace product {} for tenant {}, {} variants",
                request.externalProductId(), tenantId, request.variants().size());

        return toDto(savedMp, mappings);
    }

    // --- Marketplace Products CRUD ---

    @Transactional(readOnly = true)
    public Page<MarketplaceProductDto> getMarketplaceProducts(UUID tenantId, Pageable pageable) {
        return marketplaceProductRepository.findByTenantId(tenantId, pageable)
                .map(mp -> toDto(mp, variantMappingRepository.findByMarketplaceProductId(mp.getId())));
    }

    @Transactional(readOnly = true)
    public MarketplaceProductDto getMarketplaceProduct(UUID tenantId, UUID id) {
        MarketplaceProduct mp = findMarketplaceProduct(tenantId, id);
        List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByMarketplaceProductId(mp.getId());
        return toDto(mp, mappings);
    }

    public MarketplaceProductDto toggleExcluded(UUID tenantId, UUID id) {
        MarketplaceProduct mp = findMarketplaceProduct(tenantId, id);
        mp.setExcluded(!mp.isExcluded());
        return toDto(marketplaceProductRepository.save(mp),
                variantMappingRepository.findByMarketplaceProductId(mp.getId()));
    }

    public MarketplaceProductDto updateThreshold(UUID tenantId, UUID id, Integer threshold) {
        MarketplaceProduct mp = findMarketplaceProduct(tenantId, id);
        mp.setLowStockThreshold(threshold);
        mp.setStockAlertSent(false); // Reset alert when threshold changes
        return toDto(marketplaceProductRepository.save(mp),
                variantMappingRepository.findByMarketplaceProductId(mp.getId()));
    }

    public MarketplaceProductDto updatePricing(UUID tenantId, UUID id,
                                                String pricingRule,
                                                BigDecimal targetMarginPct,
                                                BigDecimal fixedMarkupAmount,
                                                BigDecimal minMarginPct) {
        MarketplaceProduct mp = findMarketplaceProduct(tenantId, id);
        if (pricingRule != null) {
            mp.setPricingRule(PricingRule.valueOf(pricingRule));
        }
        if (targetMarginPct != null) {
            mp.setTargetMarginPct(targetMarginPct);
        }
        if (fixedMarkupAmount != null) {
            mp.setFixedMarkupAmount(fixedMarkupAmount);
        }
        if (minMarginPct != null) {
            mp.setMinMarginPct(minMarginPct);
            mp.setMarginAlertTriggered(false);
        }
        return toDto(marketplaceProductRepository.save(mp),
                variantMappingRepository.findByMarketplaceProductId(mp.getId()));
    }

    // --- Helpers ---

    private BigDecimal calculateSellingPrice(BigDecimal sourcePrice, PricingRule rule,
                                              BigDecimal marginPct, BigDecimal fixedMarkup,
                                              BigDecimal manualPrice) {
        if (rule == PricingRule.MANUAL && manualPrice != null) {
            return manualPrice;
        }
        if (sourcePrice == null || sourcePrice.compareTo(BigDecimal.ZERO) == 0) {
            return manualPrice != null ? manualPrice : BigDecimal.ZERO;
        }
        if (rule == PricingRule.FIXED_MARKUP && fixedMarkup != null) {
            return sourcePrice.add(fixedMarkup).setScale(2, RoundingMode.HALF_UP);
        }
        // Default: MARGIN
        BigDecimal margin = marginPct != null ? marginPct : new BigDecimal("30.00");
        BigDecimal multiplier = BigDecimal.ONE.add(margin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return sourcePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateMarginCalculation(MarketplaceProduct mp, List<MarketplaceVariantMapping> mappings) {
        if (mappings.isEmpty()) return;

        BigDecimal totalSource = BigDecimal.ZERO;
        BigDecimal totalSelling = BigDecimal.ZERO;

        for (MarketplaceVariantMapping m : mappings) {
            if (m.getSourcePrice() != null && m.getVariant() != null && m.getVariant().getPrice() != null) {
                totalSource = totalSource.add(m.getSourcePrice());
                totalSelling = totalSelling.add(m.getVariant().getPrice());
            }
        }

        if (totalSource.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = totalSelling.subtract(totalSource)
                    .divide(totalSelling, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            mp.setCurrentMarginPct(margin);
        }
    }

    private String generateSlug(String name, UUID tenantId) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 200) base = base.substring(0, 200);

        String slug = base;
        int counter = 0;
        while (productRepository.existsByTenantIdAndSlug(tenantId, slug)) {
            counter++;
            slug = base + "-" + counter;
        }
        return slug;
    }

    private MarketplaceConnection findConnection(UUID tenantId, UUID connectionId) {
        return connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceConnection", connectionId));
    }

    private MarketplaceProduct findMarketplaceProduct(UUID tenantId, UUID id) {
        return marketplaceProductRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceProduct", id));
    }

    private MarketplaceProductDto toDto(MarketplaceProduct mp, List<MarketplaceVariantMapping> mappings) {
        List<MarketplaceProductDto.VariantMappingDto> mappingDtos = mappings.stream()
                .map(m -> new MarketplaceProductDto.VariantMappingDto(
                        m.getId(),
                        m.getVariant() != null ? m.getVariant().getId() : null,
                        m.getVariant() != null ? m.getVariant().getName() : null,
                        m.getCjVariantId(),
                        m.getCjSku(),
                        m.getPreviousCjSku(),
                        m.getWarehouseId(),
                        m.getWarehouseCountry(),
                        m.getSourcePrice(),
                        m.getShippingEstimate(),
                        m.getStockQuantity(),
                        m.getStockLastCheckedAt(),
                        m.getSkuStatus().name()
                ))
                .toList();

        return new MarketplaceProductDto(
                mp.getId(),
                mp.getProduct().getId(),
                mp.getProduct().getName(),
                mp.getExternalProductId(),
                mp.getSyncStatus().name(),
                mp.getPricingRule().name(),
                mp.getTargetMarginPct(),
                mp.getCurrentMarginPct(),
                mp.getMinMarginPct(),
                mp.isMarginAlertTriggered(),
                mp.isExcluded(),
                mp.getLowStockThreshold(),
                mp.isStockAlertSent(),
                mappingDtos,
                mp.getCreatedAt(),
                mp.getUpdatedAt()
        );
    }
}
