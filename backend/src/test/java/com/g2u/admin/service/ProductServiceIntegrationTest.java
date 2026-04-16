package com.g2u.admin.service;

import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.ProductDto;
import com.g2u.admin.web.dto.ProductListDto;
import com.g2u.admin.web.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import com.g2u.admin.web.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Tenant A")
                .slug("tenant-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Tenant B")
                .slug("tenant-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createProduct_shouldSucceed() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", null, "Description", null,
                "DRAFT", new BigDecimal("99.99"), null, "UAH",
                "SKU-001", null, false, 0, null, "kg",
                null, null, null, null, null, null, null
        );

        ProductDto product = productService.createProduct(tenantAId, TEST_USER_ID, request);

        assertNotNull(product.id());
        assertEquals("Test Product", product.name());
        assertEquals("test-product", product.slug());
        assertEquals("DRAFT", product.status());
        assertEquals(new BigDecimal("99.99"), product.price());
    }

    @Test
    void createAndUpdateProduct_shouldSucceed() {
        CreateProductRequest create = new CreateProductRequest(
                "Original Name", null, null, null,
                null, new BigDecimal("50.00"), null, null,
                null, null, false, null, null, null,
                null, null, null, null, null, null, null
        );

        ProductDto created = productService.createProduct(tenantAId, TEST_USER_ID, create);

        UpdateProductRequest update = new UpdateProductRequest(
                "Updated Name", null, "New description", null,
                "ACTIVE", new BigDecimal("75.00"), null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        ProductDto updated = productService.updateProduct(tenantAId, TEST_USER_ID, created.id(), update);

        assertEquals("Updated Name", updated.name());
        assertEquals("ACTIVE", updated.status());
        assertEquals(new BigDecimal("75.00"), updated.price());
        assertNotNull(updated.publishedAt());
    }

    @Test
    void tenantIsolation_productNotAccessibleByCrossTenant() {
        CreateProductRequest request = new CreateProductRequest(
                "Tenant A Product", null, null, null,
                null, null, null, null,
                null, null, false, null, null, null,
                null, null, null, null, null, null, null
        );

        ProductDto product = productService.createProduct(tenantAId, TEST_USER_ID, request);

        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProduct(tenantBId, product.id())
        );
    }

    @Test
    void tenantIsolation_listOnlyShowsOwnProducts() {
        productService.createProduct(tenantAId, TEST_USER_ID, new CreateProductRequest(
                "Product A1", null, null, null, null, null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        ));
        productService.createProduct(tenantAId, TEST_USER_ID, new CreateProductRequest(
                "Product A2", null, null, null, null, null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        ));
        productService.createProduct(tenantBId, TEST_USER_ID, new CreateProductRequest(
                "Product B1", null, null, null, null, null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        ));

        Page<ProductListDto> tenantAProducts = productService.getProducts(tenantAId, null, PageRequest.of(0, 20));
        Page<ProductListDto> tenantBProducts = productService.getProducts(tenantBId, null, PageRequest.of(0, 20));

        assertEquals(2, tenantAProducts.getTotalElements());
        assertEquals(1, tenantBProducts.getTotalElements());
    }

    @Test
    void softDelete_shouldHideProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "To Delete", null, null, null, null, null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        );

        ProductDto product = productService.createProduct(tenantAId, TEST_USER_ID, request);
        productService.deleteProduct(tenantAId, TEST_USER_ID, product.id());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProduct(tenantAId, product.id())
        );

        Page<ProductListDto> products = productService.getProducts(tenantAId, null, PageRequest.of(0, 20));
        assertEquals(0, products.getTotalElements());
    }

    @Test
    void filterByStatus_shouldWork() {
        productService.createProduct(tenantAId, TEST_USER_ID, new CreateProductRequest(
                "Draft Product", null, null, null, "DRAFT", null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        ));
        productService.createProduct(tenantAId, TEST_USER_ID, new CreateProductRequest(
                "Active Product", null, null, null, "ACTIVE", null, null, null,
                null, null, false, null, null, null, null, null, null, null, null, null, null
        ));

        Page<ProductListDto> drafts = productService.getProducts(tenantAId, ProductStatus.DRAFT, PageRequest.of(0, 20));
        Page<ProductListDto> active = productService.getProducts(tenantAId, ProductStatus.ACTIVE, PageRequest.of(0, 20));

        assertEquals(1, drafts.getTotalElements());
        assertEquals("Draft Product", drafts.getContent().get(0).name());
        assertEquals(1, active.getTotalElements());
        assertEquals("Active Product", active.getContent().get(0).name());
    }
}
