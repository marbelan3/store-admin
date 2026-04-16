package com.g2u.admin.service;

import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateProductOptionRequest;
import com.g2u.admin.web.dto.CreateProductRequest;
import com.g2u.admin.web.dto.CreateProductVariantRequest;
import com.g2u.admin.web.dto.ProductDto;
import com.g2u.admin.web.dto.ProductOptionDto;
import com.g2u.admin.web.dto.ProductVariantDetailDto;
import com.g2u.admin.web.dto.UpdateProductVariantRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductOptionServiceIntegrationTest {

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private UUID productAId;
    private UUID productBId;
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

        // Create products for each tenant
        ProductDto prodA = productService.createProduct(tenantAId, TEST_USER_ID, new CreateProductRequest(
                "T-Shirt A", null, "A t-shirt", null, null,
                new BigDecimal("29.99"), null, null, null, null,
                false, null, null, null, null, null, null, null, null, null, null
        ));
        productAId = prodA.id();

        ProductDto prodB = productService.createProduct(tenantBId, TEST_USER_ID, new CreateProductRequest(
                "T-Shirt B", null, "Another t-shirt", null, null,
                new BigDecimal("39.99"), null, null, null, null,
                false, null, null, null, null, null, null, null, null, null, null
        ));
        productBId = prodB.id();
    }

    @Test
    void createOption_shouldPersist() {
        CreateProductOptionRequest request = new CreateProductOptionRequest(
                "Size", List.of("S", "M", "L", "XL"), 0
        );

        ProductOptionDto option = productOptionService.createOption(productAId, tenantAId, request);

        assertNotNull(option.id());
        assertEquals("Size", option.name());
        assertEquals(0, option.displayOrder());
        assertEquals(4, option.values().size());
        assertEquals("S", option.values().get(0).value());
        assertEquals("M", option.values().get(1).value());
        assertEquals("L", option.values().get(2).value());
        assertEquals("XL", option.values().get(3).value());
    }

    @Test
    void getOptions_shouldReturnAllOptionsForProduct() {
        productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Size", List.of("S", "M", "L"), 0));
        productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Color", List.of("Red", "Blue"), 1));

        List<ProductOptionDto> options = productOptionService.getOptions(productAId, tenantAId);

        assertEquals(2, options.size());
        assertEquals("Size", options.get(0).name());
        assertEquals("Color", options.get(1).name());
    }

    @Test
    void deleteOption_shouldRemove() {
        ProductOptionDto option = productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Material", List.of("Cotton", "Polyester"), 0));

        productOptionService.deleteOption(option.id(), tenantAId);

        List<ProductOptionDto> options = productOptionService.getOptions(productAId, tenantAId);
        assertTrue(options.stream().noneMatch(o -> o.id().equals(option.id())));
    }

    @Test
    void createVariant_withOptionValues_shouldPersist() {
        ProductOptionDto sizeOption = productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Size", List.of("S", "M", "L"), 0));
        ProductOptionDto colorOption = productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Color", List.of("Red", "Blue"), 1));

        UUID sizeSmallId = sizeOption.values().get(0).id();
        UUID colorRedId = colorOption.values().get(0).id();

        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "Small Red", "SKU-SR-001", new BigDecimal("34.99"),
                new BigDecimal("44.99"), null, 10, 2, 0, true,
                List.of(sizeSmallId, colorRedId)
        );

        ProductVariantDetailDto variant = productOptionService.createVariant(productAId, tenantAId, request);

        assertNotNull(variant.id());
        assertEquals("Small Red", variant.name());
        assertEquals("SKU-SR-001", variant.sku());
        assertEquals(new BigDecimal("34.99"), variant.price());
        assertEquals(new BigDecimal("44.99"), variant.compareAtPrice());
        assertEquals(10, variant.quantity());
        assertTrue(variant.active());
        assertEquals(2, variant.optionValues().size());
    }

    @Test
    void updateVariant_shouldUpdatePrice() {
        CreateProductVariantRequest createReq = new CreateProductVariantRequest(
                "Default", "SKU-DEF-001", new BigDecimal("20.00"),
                null, null, 5, null, 0, true, null
        );
        ProductVariantDetailDto created = productOptionService.createVariant(productAId, tenantAId, createReq);

        UpdateProductVariantRequest updateReq = new UpdateProductVariantRequest(
                null, null, new BigDecimal("25.00"), null, null,
                null, null, null, null, null
        );
        ProductVariantDetailDto updated = productOptionService.updateVariant(created.id(), tenantAId, updateReq);

        assertEquals(new BigDecimal("25.00"), updated.price());
        assertEquals("Default", updated.name()); // unchanged
    }

    @Test
    void deleteVariant_shouldRemove() {
        CreateProductVariantRequest createReq = new CreateProductVariantRequest(
                "ToDelete", "SKU-DEL-001", new BigDecimal("10.00"),
                null, null, null, null, 0, true, null
        );
        ProductVariantDetailDto created = productOptionService.createVariant(productAId, tenantAId, createReq);

        productOptionService.deleteVariant(created.id(), tenantAId);

        List<ProductVariantDetailDto> variants = productOptionService.getVariants(productAId, tenantAId);
        assertTrue(variants.stream().noneMatch(v -> v.id().equals(created.id())));
    }

    @Test
    void tenantIsolation_variantFromTenantANotVisibleToTenantB() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "Isolated Variant", "SKU-ISO-001", new BigDecimal("15.00"),
                null, null, null, null, 0, true, null
        );
        ProductVariantDetailDto created = productOptionService.createVariant(productAId, tenantAId, request);

        // Tenant B should not see tenant A's variant
        assertThrows(ResourceNotFoundException.class, () ->
                productOptionService.updateVariant(created.id(), tenantBId,
                        new UpdateProductVariantRequest(null, null, null, null, null, null, null, null, null, null))
        );
    }

    @Test
    void createVariant_duplicateSku_shouldFail() {
        productOptionService.createVariant(productAId, tenantAId, new CreateProductVariantRequest(
                "First", "SKU-DUP-001", new BigDecimal("10.00"),
                null, null, null, null, 0, true, null
        ));

        assertThrows(IllegalArgumentException.class, () ->
                productOptionService.createVariant(productAId, tenantAId, new CreateProductVariantRequest(
                        "Second", "SKU-DUP-001", new BigDecimal("20.00"),
                        null, null, null, null, 0, true, null
                ))
        );
    }

    @Test
    void createVariant_sameSku_differentTenants_shouldSucceed() {
        productOptionService.createVariant(productAId, tenantAId, new CreateProductVariantRequest(
                "Variant A", "SKU-CROSS-001", new BigDecimal("10.00"),
                null, null, null, null, 0, true, null
        ));

        // Same SKU should be allowed for different tenant
        ProductVariantDetailDto variantB = productOptionService.createVariant(productBId, tenantBId,
                new CreateProductVariantRequest(
                        "Variant B", "SKU-CROSS-001", new BigDecimal("20.00"),
                        null, null, null, null, 0, true, null
                ));

        assertNotNull(variantB.id());
        assertEquals("SKU-CROSS-001", variantB.sku());
    }

    @Test
    void tenantIsolation_optionFromTenantANotDeletableByTenantB() {
        ProductOptionDto option = productOptionService.createOption(productAId, tenantAId,
                new CreateProductOptionRequest("Size", List.of("S", "M"), 0));

        assertThrows(ResourceNotFoundException.class, () ->
                productOptionService.deleteOption(option.id(), tenantBId)
        );
    }
}
