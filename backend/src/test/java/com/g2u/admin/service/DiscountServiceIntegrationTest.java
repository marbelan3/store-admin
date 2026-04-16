package com.g2u.admin.service;

import com.g2u.admin.domain.discount.DiscountRepository;
import com.g2u.admin.domain.discount.DiscountType;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateDiscountRequest;
import com.g2u.admin.web.dto.DiscountDto;
import com.g2u.admin.web.dto.DiscountListDto;
import com.g2u.admin.web.dto.UpdateDiscountRequest;
import com.g2u.admin.web.dto.ValidateDiscountResponse;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DiscountServiceIntegrationTest {

    @Autowired
    private DiscountService discountService;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Discount Tenant A")
                .slug("discount-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Discount Tenant B")
                .slug("discount-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createDiscount_shouldPersist() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Summer Sale", "SUMMER10", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );

        DiscountDto discount = discountService.createDiscount(tenantAId, request);

        assertNotNull(discount.id());
        assertEquals("Summer Sale", discount.name());
        assertEquals("SUMMER10", discount.code());
        assertEquals("PERCENTAGE", discount.type());
        assertEquals(0, new BigDecimal("10.00").compareTo(discount.value()));
        assertTrue(discount.active());
        assertEquals(0, discount.usageCount());
        assertNotNull(discount.createdAt());
    }

    @Test
    void createDiscount_percentageOver100_shouldFail() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Bad Discount", "BAD", DiscountType.PERCENTAGE,
                new BigDecimal("150.00"), null, null,
                null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () ->
                discountService.createDiscount(tenantAId, request));
    }

    @Test
    void createDiscount_duplicateCodeSameTenant_shouldFail() {
        CreateDiscountRequest request1 = new CreateDiscountRequest(
                "First", "UNIQUE10", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );
        discountService.createDiscount(tenantAId, request1);

        CreateDiscountRequest request2 = new CreateDiscountRequest(
                "Second", "UNIQUE10", DiscountType.FIXED_AMOUNT,
                new BigDecimal("5.00"), null, null,
                null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () ->
                discountService.createDiscount(tenantAId, request2));
    }

    @Test
    void validateCode_validDiscount_shouldReturnValid() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Valid Discount", "VALID20", DiscountType.PERCENTAGE,
                new BigDecimal("20.00"), null, null,
                null, null, null, null
        );
        discountService.createDiscount(tenantAId, request);

        ValidateDiscountResponse response = discountService.validateCode(
                tenantAId, "VALID20", new BigDecimal("100.00"));

        assertTrue(response.valid());
        assertEquals("Discount is valid", response.message());
        assertEquals("Valid Discount", response.discountName());
        assertEquals(0, new BigDecimal("20.00").compareTo(response.discountAmount()));
    }

    @Test
    void validateCode_expiredDiscount_shouldReturnInvalid() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Expired Discount", "EXPIRED", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1),
                null, null
        );
        discountService.createDiscount(tenantAId, request);

        ValidateDiscountResponse response = discountService.validateCode(
                tenantAId, "EXPIRED", new BigDecimal("100.00"));

        assertFalse(response.valid());
        assertEquals("Discount has expired", response.message());
    }

    @Test
    void validateCode_usageLimitReached_shouldReturnInvalid() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Limited Discount", "LIMITED", DiscountType.FIXED_AMOUNT,
                new BigDecimal("5.00"), null, 1,
                null, null, null, null
        );
        DiscountDto discount = discountService.createDiscount(tenantAId, request);

        // Use the discount once
        discountService.incrementUsage(discount.id(), tenantAId);

        ValidateDiscountResponse response = discountService.validateCode(
                tenantAId, "LIMITED", new BigDecimal("100.00"));

        assertFalse(response.valid());
        assertEquals("Discount usage limit reached", response.message());
    }

    @Test
    void validateCode_belowMinOrder_shouldReturnInvalid() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Min Order Discount", "MINORDER", DiscountType.PERCENTAGE,
                new BigDecimal("15.00"), new BigDecimal("50.00"), null,
                null, null, null, null
        );
        discountService.createDiscount(tenantAId, request);

        ValidateDiscountResponse response = discountService.validateCode(
                tenantAId, "MINORDER", new BigDecimal("30.00"));

        assertFalse(response.valid());
        assertTrue(response.message().contains("below minimum"));
    }

    @Test
    void validateCode_fixedAmount_shouldCapAtOrderAmount() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Big Fixed Discount", "BIGFIX", DiscountType.FIXED_AMOUNT,
                new BigDecimal("100.00"), null, null,
                null, null, null, null
        );
        discountService.createDiscount(tenantAId, request);

        ValidateDiscountResponse response = discountService.validateCode(
                tenantAId, "BIGFIX", new BigDecimal("50.00"));

        assertTrue(response.valid());
        assertEquals(0, new BigDecimal("50.00").compareTo(response.discountAmount()));
    }

    @Test
    void toggleActive_shouldUpdateStatus() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Toggle Discount", "TOGGLE", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );
        DiscountDto discount = discountService.createDiscount(tenantAId, request);
        assertTrue(discount.active());

        DiscountDto toggled = discountService.toggleActive(discount.id(), tenantAId, false);
        assertFalse(toggled.active());

        DiscountDto toggledBack = discountService.toggleActive(discount.id(), tenantAId, true);
        assertTrue(toggledBack.active());
    }

    @Test
    void tenantIsolation() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Tenant A Discount", "ISOLATE", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );
        DiscountDto discount = discountService.createDiscount(tenantAId, request);

        // Tenant B should not see Tenant A's discount
        assertThrows(ResourceNotFoundException.class, () ->
                discountService.getDiscount(discount.id(), tenantBId));

        // Tenant B should not be able to validate Tenant A's code
        ValidateDiscountResponse response = discountService.validateCode(
                tenantBId, "ISOLATE", new BigDecimal("100.00"));
        assertFalse(response.valid());
    }

    @Test
    void getDiscounts_shouldReturnPaginatedResults() {
        discountService.createDiscount(tenantAId, new CreateDiscountRequest(
                "Discount 1", null, DiscountType.PERCENTAGE,
                new BigDecimal("5.00"), null, null, null, null, null, null));
        discountService.createDiscount(tenantAId, new CreateDiscountRequest(
                "Discount 2", null, DiscountType.FIXED_AMOUNT,
                new BigDecimal("10.00"), null, null, null, null, null, null));
        discountService.createDiscount(tenantAId, new CreateDiscountRequest(
                "Discount 3", null, DiscountType.PERCENTAGE,
                new BigDecimal("20.00"), null, null, null, null, null, null));

        Page<DiscountListDto> page = discountService.getDiscounts(tenantAId, PageRequest.of(0, 2));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void updateDiscount_shouldModifyFields() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "Original Name", "ORIG", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );
        DiscountDto created = discountService.createDiscount(tenantAId, request);

        UpdateDiscountRequest updateRequest = new UpdateDiscountRequest(
                "Updated Name", "UPDATED", DiscountType.FIXED_AMOUNT,
                new BigDecimal("25.00"), new BigDecimal("50.00"), 100,
                null, null, null, null, null
        );
        DiscountDto updated = discountService.updateDiscount(created.id(), tenantAId, updateRequest);

        assertEquals("Updated Name", updated.name());
        assertEquals("UPDATED", updated.code());
        assertEquals("FIXED_AMOUNT", updated.type());
        assertEquals(0, new BigDecimal("25.00").compareTo(updated.value()));
        assertEquals(0, new BigDecimal("50.00").compareTo(updated.minOrderAmount()));
        assertEquals(100, updated.usageLimit());
    }

    @Test
    void deleteDiscount_shouldRemove() {
        CreateDiscountRequest request = new CreateDiscountRequest(
                "To Delete", "DELETE", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), null, null,
                null, null, null, null
        );
        DiscountDto created = discountService.createDiscount(tenantAId, request);

        discountService.deleteDiscount(created.id(), tenantAId);

        assertThrows(ResourceNotFoundException.class, () ->
                discountService.getDiscount(created.id(), tenantAId));
    }
}
