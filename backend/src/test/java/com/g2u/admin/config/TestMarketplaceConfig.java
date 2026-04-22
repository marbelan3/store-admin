package com.g2u.admin.config;

import com.g2u.admin.infrastructure.marketplace.CjAuthException;
import com.g2u.admin.infrastructure.marketplace.MarketplaceAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TestConfiguration
public class TestMarketplaceConfig {

    public static final AtomicBoolean FAIL_AUTH = new AtomicBoolean(false);
    public static final AtomicBoolean FAIL_SEARCH = new AtomicBoolean(false);

    public static void resetFlags() {
        FAIL_AUTH.set(false);
        FAIL_SEARCH.set(false);
    }

    @Bean
    @Primary
    public MarketplaceAdapter testMarketplaceAdapter() {
        return new MarketplaceAdapter() {
            @Override
            public String authenticate(String apiKey) {
                if (FAIL_AUTH.get()) throw new CjAuthException("Invalid API key");
                return "test-access-token-" + apiKey.hashCode();
            }

            @Override
            public List<CjProduct> searchProducts(String accessToken, String query, int page, int pageSize) {
                if (FAIL_SEARCH.get()) throw new CjAuthException("Token expired");
                return List.of(new CjProduct("test-pid", "Test Product " + query, "https://img.test/product.jpg",
                        "Test description", "Electronics", new BigDecimal("10.00"),
                        List.of(new CjVariant("vid-1", "Variant A", "SKU-1", new BigDecimal("10.00"), "https://img.test/v1.jpg", 100),
                                new CjVariant("vid-2", "Variant B", "SKU-2", new BigDecimal("12.00"), "https://img.test/v2.jpg", 50))));
            }

            @Override
            public CjProduct getProductDetails(String accessToken, String externalProductId) {
                return new CjProduct(externalProductId, "Test Product", "https://img.test/product.jpg",
                        "Test description", "Electronics", new BigDecimal("10.00"),
                        List.of(new CjVariant("vid-1", "Variant A", "SKU-1", new BigDecimal("10.00"), "https://img.test/v1.jpg", 100),
                                new CjVariant("vid-2", "Variant B", "SKU-2", new BigDecimal("12.00"), "https://img.test/v2.jpg", 50)));
            }

            @Override
            public List<CjVariant> getVariants(String accessToken, String externalProductId) {
                return List.of(
                        new CjVariant("vid-1", "Variant A", "SKU-1", new BigDecimal("10.00"), "", 100),
                        new CjVariant("vid-2", "Variant B", "SKU-2", new BigDecimal("12.00"), "", 50));
            }

            @Override
            public int getStock(String accessToken, String cjSku) { return 100; }

            @Override
            public BigDecimal calculateShipping(String accessToken, String cjSku, String warehouseId,
                                                 String destinationCountry, int quantity) {
                return new BigDecimal("5.00");
            }

            @Override
            public CjOrderResult placeOrder(String accessToken, CjOrderRequest orderRequest) {
                return new CjOrderResult("ord-123", "ORD-123", "CREATED");
            }

            @Override
            public CjOrderStatus getOrderStatus(String accessToken, String cjOrderId) {
                return new CjOrderStatus(cjOrderId, "SHIPPED", "TRK123");
            }

            @Override
            public CjTrackingInfo getTracking(String accessToken, String cjOrderId) {
                return new CjTrackingInfo("TRK123", "DHL", Collections.emptyList());
            }
        };
    }
}
