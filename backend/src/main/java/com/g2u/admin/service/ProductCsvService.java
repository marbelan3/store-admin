package com.g2u.admin.service;

import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.util.SlugUtils;
import com.g2u.admin.web.dto.ImportResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductCsvService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCsvService.class);

    private static final String[] CSV_HEADERS = {
            "name", "description", "shortDescription", "status", "price",
            "compareAtPrice", "currency", "sku", "barcode",
            "trackInventory", "quantity"
    };

    private static final int MAX_IMPORT_ROWS = 1000;

    private final ProductRepository productRepository;

    public ProductCsvService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public String exportProducts(UUID tenantId) {
        List<Product> products = productRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", CSV_HEADERS)).append("\n");

        for (Product p : products) {
            sb.append(escapeCsv(p.getName())).append(",");
            sb.append(escapeCsv(p.getDescription())).append(",");
            sb.append(escapeCsv(p.getShortDescription())).append(",");
            sb.append(escapeCsv(p.getStatus() != null ? p.getStatus().name() : "")).append(",");
            sb.append(p.getPrice() != null ? p.getPrice().toPlainString() : "").append(",");
            sb.append(p.getCompareAtPrice() != null ? p.getCompareAtPrice().toPlainString() : "").append(",");
            sb.append(escapeCsv(p.getCurrency())).append(",");
            sb.append(escapeCsv(p.getSku())).append(",");
            sb.append(escapeCsv(p.getBarcode())).append(",");
            sb.append(p.isTrackInventory()).append(",");
            sb.append(p.getQuantity() != null ? p.getQuantity() : 0);
            sb.append("\n");
        }

        return sb.toString();
    }

    public ImportResultDto importProducts(UUID tenantId, MultipartFile file) throws IOException {
        List<ImportResultDto.ImportError> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int rowNum = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add(new ImportResultDto.ImportError(0, "Empty CSV file"));
                return new ImportResultDto(0, 0, errors);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                rowNum++;

                if (rowNum > MAX_IMPORT_ROWS) {
                    errors.add(new ImportResultDto.ImportError(rowNum, "Maximum of " + MAX_IMPORT_ROWS + " rows exceeded"));
                    break;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    List<String> fields = parseCsvLine(line);

                    String name = getField(fields, 0);
                    if (name == null || name.isBlank()) {
                        errors.add(new ImportResultDto.ImportError(rowNum, "Name is required"));
                        continue;
                    }

                    String description = getField(fields, 1);
                    String shortDescription = getField(fields, 2);
                    String statusStr = getField(fields, 3);
                    String priceStr = getField(fields, 4);
                    String compareAtPriceStr = getField(fields, 5);
                    String currency = getField(fields, 6);
                    String sku = getField(fields, 7);
                    String barcode = getField(fields, 8);
                    String trackInventoryStr = getField(fields, 9);
                    String quantityStr = getField(fields, 10);

                    ProductStatus status = parseStatus(statusStr);
                    BigDecimal price = parseBigDecimal(priceStr);
                    BigDecimal compareAtPrice = parseBigDecimal(compareAtPriceStr);
                    boolean trackInventory = "true".equalsIgnoreCase(trackInventoryStr);
                    Integer quantity = parseInteger(quantityStr);

                    // Match by SKU for update
                    Product existing = null;
                    if (sku != null && !sku.isBlank()) {
                        existing = productRepository.findByTenantIdAndSku(tenantId, sku).orElse(null);
                    }

                    if (existing != null) {
                        existing.setName(name);
                        if (description != null) existing.setDescription(description);
                        if (shortDescription != null) existing.setShortDescription(shortDescription);
                        if (status != null) existing.setStatus(status);
                        if (price != null) existing.setPrice(price);
                        if (compareAtPrice != null) existing.setCompareAtPrice(compareAtPrice);
                        if (currency != null && !currency.isBlank()) existing.setCurrency(currency);
                        if (barcode != null) existing.setBarcode(barcode);
                        existing.setTrackInventory(trackInventory);
                        if (quantity != null) existing.setQuantity(quantity);
                        productRepository.save(existing);
                        updated++;
                    } else {
                        String baseSlug = SlugUtils.generateSlug(name);
                        String slug = SlugUtils.ensureUnique(baseSlug,
                                s -> productRepository.existsByTenantIdAndSlug(tenantId, s));

                        Product product = Product.builder()
                                .name(name)
                                .slug(slug)
                                .description(description)
                                .shortDescription(shortDescription)
                                .status(status != null ? status : ProductStatus.DRAFT)
                                .price(price)
                                .compareAtPrice(compareAtPrice)
                                .currency(currency != null && !currency.isBlank() ? currency : "UAH")
                                .sku(sku)
                                .barcode(barcode)
                                .trackInventory(trackInventory)
                                .quantity(quantity != null ? quantity : 0)
                                .build();
                        product.setTenantId(tenantId);
                        productRepository.save(product);
                        created++;
                    }

                } catch (Exception e) {
                    LOG.warn("Error processing CSV row {}: {}", rowNum, e.getMessage());
                    errors.add(new ImportResultDto.ImportError(rowNum, e.getMessage()));
                }
            }
        }

        return new ImportResultDto(created, updated, errors);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());

        return fields;
    }

    private String getField(List<String> fields, int index) {
        if (index >= fields.size()) {
            return null;
        }
        String val = fields.get(index);
        return val.isEmpty() ? null : val;
    }

    private ProductStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProductStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
