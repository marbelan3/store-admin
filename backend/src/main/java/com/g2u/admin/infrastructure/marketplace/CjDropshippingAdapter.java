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

@Component
public class CjDropshippingAdapter implements MarketplaceAdapter {

    private static final Logger log = LoggerFactory.getLogger(CjDropshippingAdapter.class);
    private static final String BASE_URL = "https://developers.cjdropshipping.com/api2.0/v1";

    // Minimum interval between CJ API calls (ms). CJ allows 1-6 req/sec depending on plan.
    private static final long MIN_REQUEST_INTERVAL_MS = 1200;
    private volatile long lastRequestTime = 0;
    private final Object rateLock = new Object();
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CjDropshippingAdapter(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public String authenticate(String apiKey) {
        try {
            acquireRateLimit();
            var body = Map.of("apiKey", apiKey);
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
    public List<CjProduct> listProductsByCategory(String accessToken, String categoryId, int page, int pageSize) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/list?categoryId=" + categoryId
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
    public List<CjCategory> getCategories(String accessToken) {
        try {
            acquireRateLimit();
            String url = BASE_URL + "/product/getCategory";
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            List<CjCategory> categories = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode first : node) {
                    List<CjSubCategory> subs = new ArrayList<>();
                    for (JsonNode second : first.path("categoryFirstList")) {
                        List<CjLeafCategory> leaves = new ArrayList<>();
                        for (JsonNode leaf : second.path("categorySecondList")) {
                            leaves.add(new CjLeafCategory(
                                    leaf.path("categoryId").asText(),
                                    leaf.path("categoryName").asText()));
                        }
                        subs.add(new CjSubCategory(second.path("categorySecondName").asText(), leaves));
                    }
                    categories.add(new CjCategory(first.path("categoryFirstName").asText(), subs));
                }
            }
            return categories;
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
            String url = BASE_URL + "/product/stock/queryByVid?vid=" + cjSku;
            var response = exchange(accessToken, url, HttpMethod.GET, null);
            var node = extractData(response);
            // Response is an array of warehouse stocks; sum up storageNum across all warehouses
            int total = 0;
            if (node.isArray()) {
                for (JsonNode warehouse : node) {
                    total += warehouse.path("storageNum").asInt(0);
                }
            }
            return total;
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
                    .map(i -> {
                        var m = new java.util.HashMap<String, Object>();
                        m.put("vid", i.sku());
                        m.put("quantity", i.quantity());
                        if (i.shippingMethod() != null && !i.shippingMethod().isEmpty()) {
                            m.put("logisticName", i.shippingMethod());
                        }
                        return m;
                    })
                    .toList();
            var body = new java.util.HashMap<String, Object>();
            body.put("fromCountryCode", orderRequest.fromCountryCode() != null ? orderRequest.fromCountryCode() : "CN");
            body.put("shippingCountryCode", orderRequest.shippingCountry());
            body.put("shippingProvince", orderRequest.shippingProvince());
            body.put("shippingCity", orderRequest.shippingCity());
            body.put("shippingAddress", orderRequest.shippingAddress());
            body.put("shippingZip", orderRequest.shippingZip());
            body.put("shippingCustomerName", orderRequest.shippingCustomerName());
            body.put("shippingPhone", orderRequest.shippingPhone());
            body.put("products", items);
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

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_BACKOFF_MS = 3000;

    private ResponseEntity<JsonNode> exchange(String accessToken, String url, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("CJ-Access-Token", accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restTemplate.exchange(url, method, entity, JsonNode.class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 && attempt < MAX_RETRIES) {
                    log.warn("CJ API 429, retry {}/{} after {}ms", attempt + 1, MAX_RETRIES, RETRY_BACKOFF_MS);
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CjApiException("Retry interrupted", ie);
                    }
                    continue;
                }
                throw e;
            }
        }
        throw new CjApiException("Unexpected: retries exhausted");
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
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < MIN_REQUEST_INTERVAL_MS) {
                try {
                    Thread.sleep(MIN_REQUEST_INTERVAL_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CjApiException("Rate limit wait interrupted", e);
                }
            }
            lastRequestTime = System.currentTimeMillis();
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
        // CJ returns bigImage (single URL) on detail, productImage (single URL on list, JSON array on detail)
        String image = node.path("bigImage").asText("");
        if (image.isEmpty()) {
            String rawImage = node.path("productImage").asText("");
            // On detail endpoint productImage is a JSON array string — extract first URL
            if (rawImage.startsWith("[")) {
                try {
                    JsonNode arr = objectMapper.readTree(rawImage);
                    if (arr.isArray() && !arr.isEmpty()) {
                        image = arr.get(0).asText();
                    }
                } catch (Exception e) {
                    image = rawImage;
                }
            } else {
                image = rawImage;
            }
        }
        // Description is in "remark" field (HTML content)
        String description = node.path("description").asText("");
        if (description.isEmpty()) {
            description = node.path("remark").asText("");
        }
        return new CjProduct(
                node.path("pid").asText(),
                node.path("productNameEn").asText(),
                image,
                description,
                node.path("categoryName").asText(""),
                parseSellPrice(node.path("sellPrice").asText("0")),
                variants
        );
    }

    /**
     * CJ list endpoint returns sellPrice as a range like "0.31 -- 0.32",
     * while detail endpoint returns a plain number. Handle both.
     */
    private BigDecimal parseSellPrice(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        // Take the first number from a range like "0.31 -- 0.32"
        String first = raw.split("\\s*--\\s*")[0].trim();
        try {
            return new BigDecimal(first);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private CjVariant mapVariant(JsonNode v) {
        // Stock is not on variant directly; use inventoryNum if present, otherwise 0 (fetched separately via getStock)
        int stock = v.path("inventoryNum").asInt(0);
        return new CjVariant(
                v.path("vid").asText(),
                v.path("variantNameEn").asText(),
                v.path("variantSku").asText(),
                parseSellPrice(v.path("variantSellPrice").asText("0")),
                v.path("variantImage").asText(""),
                stock
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
