package com.g2u.admin.infrastructure.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class CjDropshippingAdapter implements MarketplaceAdapter {

    private static final Logger log = LoggerFactory.getLogger(CjDropshippingAdapter.class);
    private static final String BASE_URL = "https://developers.cjdropshipping.com/api2.0/v1";

    // Rate limiter: 1 request per second (conservative, CJ allows 1-6 depending on plan)
    private final Semaphore rateLimiter = new Semaphore(1);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CjDropshippingAdapter(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public String authenticate(String email, String apiKey) {
        try {
            acquireRateLimit();
            var body = Map.of("email", email, "password", apiKey);
            var response = restTemplate.postForEntity(
                    BASE_URL + "/authentication/getAccessToken",
                    body,
                    JsonNode.class);
            var node = extractData(response);
            return node.path("accessToken").asText();
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public List<CjProduct> searchProducts(String accessToken, String query, int page, int pageSize) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/list?productNameEn=" + query
                    + "&pageNum=" + page + "&pageSize=" + pageSize;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            var list = node.path("list");
            List<CjProduct> products = new ArrayList<>();
            if (list.isArray()) {
                for (JsonNode item : list) {
                    products.add(mapProduct(item));
                }
            }
            return products;
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public CjProduct getProductDetails(String accessToken, String externalProductId) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/query?pid=" + externalProductId;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            return mapProduct(extractData(response));
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public List<CjVariant> getVariants(String accessToken, String externalProductId) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/variant/query?pid=" + externalProductId;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            List<CjVariant> variants = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode v : node) {
                    variants.add(mapVariant(v));
                }
            }
            return variants;
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public int getStock(String accessToken, String cjSku) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/stock?vid=" + cjSku;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            return node.path("stockQuantity").asInt(0);
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public BigDecimal calculateShipping(String accessToken, String cjSku, String warehouseId,
                                         String destinationCountry, int quantity) {
        try {
            acquireRateLimit();
            var body = Map.of(
                    "startCountryCode", warehouseId != null ? warehouseId : "CN",
                    "endCountryCode", destinationCountry,
                    "products", List.of(Map.of("vid", cjSku, "quantity", quantity))
            );
            var response = exchange(accessToken, BASE_URL + "/logistic/freightCalculate", HttpMethod.POST, body);
            var node = extractData(response);
            if (node.isArray() && !node.isEmpty()) {
                return new BigDecimal(node.get(0).path("logisticPrice").asText("0"));
            }
            return BigDecimal.ZERO;
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public CjOrderResult placeOrder(String accessToken, CjOrderRequest orderRequest) {
        try {
            acquireRateLimit();
            var items = orderRequest.items().stream()
                    .map(i -> Map.of(
                            "sku", i.sku(),
                            "quantity", i.quantity(),
                            "shippingMethod", i.shippingMethod() != null ? i.shippingMethod() : ""
                    ))
                    .toList();
            var body = Map.of(
                    "shippingCountry", orderRequest.shippingCountry(),
                    "shippingProvince", orderRequest.shippingProvince(),
                    "shippingCity", orderRequest.shippingCity(),
                    "shippingAddress", orderRequest.shippingAddress(),
                    "shippingZip", orderRequest.shippingZip(),
                    "shippingCustomerName", orderRequest.shippingCustomerName(),
                    "shippingPhone", orderRequest.shippingPhone(),
                    "orderItems", items
            );
            var response = exchange(accessToken, BASE_URL + "/shopping/order/createOrder", HttpMethod.POST, body);
            var node = extractData(response);
            return new CjOrderResult(
                    node.path("orderId").asText(),
                    node.path("orderNum").asText(),
                    node.path("status").asText("CREATED")
            );
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public CjOrderStatus getOrderStatus(String accessToken, String cjOrderId) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/shopping/order/getOrderDetail?orderId=" + cjOrderId;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            return new CjOrderStatus(
                    node.path("orderId").asText(),
                    node.path("orderStatus").asText(),
                    node.path("trackNumber").asText(null)
            );
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    @Override
    public CjTrackingInfo getTracking(String accessToken, String cjOrderId) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/logistic/getTrackInfo?orderNum=" + cjOrderId;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            return new CjTrackingInfo(
                    node.path("trackNumber").asText(),
                    node.path("logisticName").asText(),
                    Collections.emptyList()
            );
        } catch (HttpClientErrorException e) {
            throw mapHttpError(e);
        }
    }

    // --- Internal helpers ---

    private ResponseEntity<JsonNode> exchange(String accessToken, String url, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("CJ-Access-Token", accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, method, entity, JsonNode.class);
    }

    private JsonNode extractData(ResponseEntity<JsonNode> response) {
        JsonNode body = response.getBody();
        if (body == null) {
            throw new CjApiException("Empty response from CJ API");
        }
        boolean success = body.path("result").asBoolean(false);
        int code = body.path("code").asInt(0);
        if (!success && code == 1600100) {
            throw new CjAuthException("CJ access token is invalid or expired");
        }
        if (!success) {
            String message = body.path("message").asText("Unknown CJ API error");
            throw new CjApiException(code, message);
        }
        return body.path("data");
    }

    private void acquireRateLimit() {
        try {
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new CjRateLimitException();
            }
            // Release after 1 second to enforce rate limit
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                rateLimiter.release();
            }).start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CjApiException("Rate limit acquisition interrupted", e);
        }
    }

    private CjProduct mapProduct(JsonNode node) {
        List<CjVariant> variants = new ArrayList<>();
        JsonNode variantsNode = node.path("variants");
        if (variantsNode.isArray()) {
            for (JsonNode v : variantsNode) {
                variants.add(mapVariant(v));
            }
        }
        return new CjProduct(
                node.path("pid").asText(),
                node.path("productNameEn").asText(),
                node.path("productImage").asText(),
                node.path("description").asText(""),
                node.path("categoryName").asText(""),
                new BigDecimal(node.path("sellPrice").asText("0")),
                variants
        );
    }

    private CjVariant mapVariant(JsonNode v) {
        return new CjVariant(
                v.path("vid").asText(),
                v.path("variantNameEn").asText(),
                v.path("variantSku").asText(),
                new BigDecimal(v.path("variantSellPrice").asText("0")),
                v.path("variantImage").asText(""),
                v.path("variantVolume").asInt(0)
        );
    }

    private CjApiException mapHttpError(HttpClientErrorException e) {
        if (e.getStatusCode().value() == 429) {
            return new CjRateLimitException();
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return new CjAuthException("CJ authentication failed: " + e.getMessage());
        }
        return new CjApiException(e.getStatusCode().value(), "CJ API error: " + e.getMessage());
    }
}
