package com.g2u.admin.infrastructure.marketplace;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Adapter interface for marketplace integrations (CJ Dropshipping, AliExpress, etc.).
 */
public interface MarketplaceAdapter {

    /**
     * Authenticate and obtain an access token.
     * @param email the account email for the marketplace
     * @param apiKey the API key / password for the marketplace
     * @return access token string
     */
    String authenticate(String email, String apiKey);

    /**
     * Search products in the marketplace catalog.
     */
    List<CjProduct> searchProducts(String accessToken, String query, int page, int pageSize);

    /**
     * Get detailed product info by external product ID.
     */
    CjProduct getProductDetails(String accessToken, String externalProductId);

    /**
     * Get variants for a product.
     */
    List<CjVariant> getVariants(String accessToken, String externalProductId);

    /**
     * Get stock quantity for a specific SKU/variant.
     */
    int getStock(String accessToken, String cjSku);

    /**
     * Calculate shipping cost for a product to a destination.
     */
    BigDecimal calculateShipping(String accessToken, String cjSku, String warehouseId, String destinationCountry, int quantity);

    /**
     * Place an order with the marketplace.
     */
    CjOrderResult placeOrder(String accessToken, CjOrderRequest orderRequest);

    /**
     * Get order status by CJ order ID.
     */
    CjOrderStatus getOrderStatus(String accessToken, String cjOrderId);

    /**
     * Get tracking info for an order.
     */
    CjTrackingInfo getTracking(String accessToken, String cjOrderId);

    // --- Inner DTOs ---

    record CjProduct(
            String pid,
            String productName,
            String productImage,
            String description,
            String categoryName,
            BigDecimal sellPrice,
            List<CjVariant> variants
    ) {}

    record CjVariant(
            String vid,
            String variantName,
            String variantSku,
            BigDecimal variantSellPrice,
            String variantImage,
            int stock
    ) {}

    record CjOrderRequest(
            String shippingCountry,
            String shippingProvince,
            String shippingCity,
            String shippingAddress,
            String shippingZip,
            String shippingCustomerName,
            String shippingPhone,
            List<CjOrderItem> items
    ) {}

    record CjOrderItem(
            String sku,
            int quantity,
            String shippingMethod
    ) {}

    record CjOrderResult(
            String orderId,
            String orderNumber,
            String status
    ) {}

    record CjOrderStatus(
            String orderId,
            String status,
            String trackingNumber
    ) {}

    record CjTrackingInfo(
            String trackingNumber,
            String logisticsProvider,
            List<Map<String, String>> trackingEvents
    ) {}
}
