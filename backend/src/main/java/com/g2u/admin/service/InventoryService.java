package com.g2u.admin.service;

import com.g2u.admin.domain.marketplace.MarketplaceVariantMapping;
import com.g2u.admin.domain.marketplace.MarketplaceVariantMappingRepository;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.domain.product.ProductVariantRepository;
import com.g2u.admin.web.dto.BulkInventoryUpdateRequest;
import com.g2u.admin.web.dto.InventoryItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MarketplaceVariantMappingRepository variantMappingRepository;
    private final NotificationService notificationService;
    private final WebhookService webhookService;

    public InventoryService(ProductRepository productRepository,
                            ProductVariantRepository productVariantRepository,
                            MarketplaceVariantMappingRepository variantMappingRepository,
                            NotificationService notificationService,
                            WebhookService webhookService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.variantMappingRepository = variantMappingRepository;
        this.notificationService = notificationService;
        this.webhookService = webhookService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemDto> getInventory(UUID tenantId, Boolean lowStock, Boolean outOfStock, Pageable pageable) {
        List<Product> products = productRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
        List<ProductVariant> variants = productVariantRepository.findByTenantId(tenantId);

        // Build variantId -> lastSyncedAt map from marketplace variant mappings
        List<MarketplaceVariantMapping> mappings = variantMappingRepository.findByTenantId(tenantId);
        Map<UUID, Instant> variantSyncMap = mappings.stream()
                .filter(m -> m.getVariant() != null && m.getStockLastCheckedAt() != null)
                .collect(Collectors.toMap(
                        m -> m.getVariant().getId(),
                        MarketplaceVariantMapping::getStockLastCheckedAt,
                        (a, b) -> a.isAfter(b) ? a : b // keep latest
                ));

        List<InventoryItemDto> items = new ArrayList<>();

        // Collect product IDs that have variants
        Set<UUID> productsWithVariants = variants.stream()
                .map(v -> v.getProduct().getId())
                .collect(Collectors.toSet());

        // Only show product-level row if it has NO variants
        for (Product p : products) {
            if (productsWithVariants.contains(p.getId())) continue;
            items.add(new InventoryItemDto(
                    p.getId(),
                    "PRODUCT",
                    p.getName(),
                    null,
                    p.getSku(),
                    p.getQuantity(),
                    p.getLowStockThreshold(),
                    p.isTrackInventory(),
                    null
            ));
        }

        for (ProductVariant v : variants) {
            Product parent = products.stream()
                    .filter(p -> p.getId().equals(v.getProduct().getId()))
                    .findFirst()
                    .orElse(null);

            items.add(new InventoryItemDto(
                    v.getId(),
                    "VARIANT",
                    parent != null ? parent.getName() : null,
                    v.getName(),
                    v.getSku(),
                    v.getQuantity(),
                    v.getLowStockThreshold(),
                    parent != null && parent.isTrackInventory(),
                    variantSyncMap.get(v.getId())
            ));
        }

        // Apply filters
        if (Boolean.TRUE.equals(lowStock)) {
            items = items.stream()
                    .filter(i -> i.lowStockThreshold() != null
                            && i.quantity() != null
                            && i.quantity() <= i.lowStockThreshold())
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(outOfStock)) {
            items = items.stream()
                    .filter(i -> i.quantity() == null || i.quantity() == 0)
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());

        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }

        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    public List<InventoryItemDto> bulkUpdateQuantities(UUID tenantId, BulkInventoryUpdateRequest request) {
        List<BulkInventoryUpdateRequest.InventoryUpdateItem> productUpdates = request.items().stream()
                .filter(i -> "PRODUCT".equalsIgnoreCase(i.type()))
                .toList();

        List<BulkInventoryUpdateRequest.InventoryUpdateItem> variantUpdates = request.items().stream()
                .filter(i -> "VARIANT".equalsIgnoreCase(i.type()))
                .toList();

        List<InventoryItemDto> results = new ArrayList<>();

        if (!productUpdates.isEmpty()) {
            List<UUID> productIds = productUpdates.stream().map(BulkInventoryUpdateRequest.InventoryUpdateItem::id).toList();
            List<Product> products = productRepository.findByTenantIdAndIdIn(tenantId, productIds);
            Map<UUID, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            for (BulkInventoryUpdateRequest.InventoryUpdateItem item : productUpdates) {
                Product product = productMap.get(item.id());
                if (product != null) {
                    product.setQuantity(item.quantity());
                    productRepository.save(product);
                    results.add(new InventoryItemDto(
                            product.getId(),
                            "PRODUCT",
                            product.getName(),
                            null,
                            product.getSku(),
                            product.getQuantity(),
                            product.getLowStockThreshold(),
                            product.isTrackInventory(),
                            null
                    ));

                    // Check low stock threshold and create notification
                    checkLowStock(tenantId, product.getName(), product.getQuantity(), product.getLowStockThreshold());
                }
            }
        }

        if (!variantUpdates.isEmpty()) {
            List<UUID> variantIds = variantUpdates.stream().map(BulkInventoryUpdateRequest.InventoryUpdateItem::id).toList();
            List<ProductVariant> variants = productVariantRepository.findByTenantIdAndIdIn(tenantId, variantIds);
            Map<UUID, ProductVariant> variantMap = variants.stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

            for (BulkInventoryUpdateRequest.InventoryUpdateItem item : variantUpdates) {
                ProductVariant variant = variantMap.get(item.id());
                if (variant != null) {
                    variant.setQuantity(item.quantity());
                    productVariantRepository.save(variant);
                    results.add(new InventoryItemDto(
                            variant.getId(),
                            "VARIANT",
                            variant.getProduct().getName(),
                            variant.getName(),
                            variant.getSku(),
                            variant.getQuantity(),
                            variant.getLowStockThreshold(),
                            variant.getProduct().isTrackInventory(),
                            null
                    ));
                }
            }
        }

        // Dispatch webhook for bulk inventory update
        webhookService.dispatch(tenantId, "inventory.updated",
                Map.of("updatedCount", results.size()));

        return results;
    }

    private void checkLowStock(UUID tenantId, String productName, Integer quantity, Integer threshold) {
        if (threshold != null && quantity != null && quantity <= threshold) {
            LOG.debug("Low stock detected for product '{}': quantity={}, threshold={}", productName, quantity, threshold);
            notificationService.createNotification(
                    tenantId,
                    null,
                    NotificationType.LOW_STOCK,
                    "Low stock: " + productName,
                    "Product '" + productName + "' has " + quantity + " units remaining (threshold: " + threshold + ")"
            );
        }
    }
}
