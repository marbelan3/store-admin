package com.g2u.admin.service;

import com.g2u.admin.domain.discount.Discount;
import com.g2u.admin.domain.discount.DiscountCategory;
import com.g2u.admin.domain.discount.DiscountCategoryRepository;
import com.g2u.admin.domain.discount.DiscountProduct;
import com.g2u.admin.domain.discount.DiscountProductRepository;
import com.g2u.admin.domain.discount.DiscountRepository;
import com.g2u.admin.domain.discount.DiscountType;
import com.g2u.admin.web.dto.CreateDiscountRequest;
import com.g2u.admin.web.dto.DiscountDto;
import com.g2u.admin.web.dto.DiscountListDto;
import com.g2u.admin.web.dto.UpdateDiscountRequest;
import com.g2u.admin.web.dto.ValidateDiscountResponse;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DiscountService {

    private static final Logger log = LoggerFactory.getLogger(DiscountService.class);

    private final DiscountRepository discountRepository;
    private final DiscountProductRepository discountProductRepository;
    private final DiscountCategoryRepository discountCategoryRepository;

    public DiscountService(DiscountRepository discountRepository,
                           DiscountProductRepository discountProductRepository,
                           DiscountCategoryRepository discountCategoryRepository) {
        this.discountRepository = discountRepository;
        this.discountProductRepository = discountProductRepository;
        this.discountCategoryRepository = discountCategoryRepository;
    }

    public DiscountDto createDiscount(UUID tenantId, CreateDiscountRequest request) {
        validateDiscountValues(request.type(), request.value());

        if (request.code() != null && !request.code().isBlank()) {
            if (discountRepository.existsByCodeAndTenantId(request.code(), tenantId)) {
                throw new IllegalArgumentException("Discount code already exists for this tenant: " + request.code());
            }
        }

        Discount discount = Discount.builder()
                .name(request.name())
                .code(request.code() != null && !request.code().isBlank() ? request.code() : null)
                .type(request.type())
                .value(request.value())
                .minOrderAmount(request.minOrderAmount())
                .usageLimit(request.usageLimit())
                .usageCount(0)
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .active(true)
                .discountProducts(new ArrayList<>())
                .discountCategories(new ArrayList<>())
                .build();
        discount.setTenantId(tenantId);

        discount = discountRepository.save(discount);

        if (request.productIds() != null && !request.productIds().isEmpty()) {
            for (UUID productId : request.productIds()) {
                DiscountProduct dp = DiscountProduct.builder()
                        .discount(discount)
                        .productId(productId)
                        .build();
                discount.getDiscountProducts().add(dp);
            }
        }

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            for (UUID categoryId : request.categoryIds()) {
                DiscountCategory dc = DiscountCategory.builder()
                        .discount(discount)
                        .categoryId(categoryId)
                        .build();
                discount.getDiscountCategories().add(dc);
            }
        }

        discount = discountRepository.save(discount);
        log.info("Created discount '{}' (id={}) for tenant {}", discount.getName(), discount.getId(), tenantId);

        return toDto(discount);
    }

    @Transactional(readOnly = true)
    public DiscountDto getDiscount(UUID id, UUID tenantId) {
        Discount discount = discountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        return toDto(discount);
    }

    @Transactional(readOnly = true)
    public Page<DiscountListDto> getDiscounts(UUID tenantId, Pageable pageable) {
        return discountRepository.findByTenantId(tenantId, pageable)
                .map(this::toListDto);
    }

    public DiscountDto updateDiscount(UUID id, UUID tenantId, UpdateDiscountRequest request) {
        Discount discount = discountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));

        if (request.name() != null) discount.setName(request.name());
        if (request.code() != null) {
            if (!request.code().isBlank()) {
                discountRepository.findByCodeAndTenantId(request.code(), tenantId)
                        .filter(existing -> !existing.getId().equals(id))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Discount code already exists for this tenant: " + request.code());
                        });
                discount.setCode(request.code());
            } else {
                discount.setCode(null);
            }
        }
        if (request.type() != null) discount.setType(request.type());
        if (request.value() != null) {
            DiscountType effectiveType = request.type() != null ? request.type() : discount.getType();
            validateDiscountValues(effectiveType, request.value());
            discount.setValue(request.value());
        }
        if (request.minOrderAmount() != null) discount.setMinOrderAmount(request.minOrderAmount());
        if (request.usageLimit() != null) discount.setUsageLimit(request.usageLimit());
        if (request.startsAt() != null) discount.setStartsAt(request.startsAt());
        if (request.endsAt() != null) discount.setEndsAt(request.endsAt());
        if (request.active() != null) discount.setActive(request.active());

        if (request.productIds() != null) {
            discount.getDiscountProducts().clear();
            for (UUID productId : request.productIds()) {
                DiscountProduct dp = DiscountProduct.builder()
                        .discount(discount)
                        .productId(productId)
                        .build();
                discount.getDiscountProducts().add(dp);
            }
        }

        if (request.categoryIds() != null) {
            discount.getDiscountCategories().clear();
            for (UUID categoryId : request.categoryIds()) {
                DiscountCategory dc = DiscountCategory.builder()
                        .discount(discount)
                        .categoryId(categoryId)
                        .build();
                discount.getDiscountCategories().add(dc);
            }
        }

        discount = discountRepository.save(discount);
        log.info("Updated discount '{}' (id={}) for tenant {}", discount.getName(), id, tenantId);

        return toDto(discount);
    }

    public void deleteDiscount(UUID id, UUID tenantId) {
        Discount discount = discountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        discountRepository.delete(discount);
        log.info("Deleted discount '{}' (id={}) from tenant {}", discount.getName(), id, tenantId);
    }

    public DiscountDto toggleActive(UUID id, UUID tenantId, boolean active) {
        Discount discount = discountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        discount.setActive(active);
        discount = discountRepository.save(discount);
        log.info("Toggled discount '{}' (id={}) active={} for tenant {}", discount.getName(), id, active, tenantId);
        return toDto(discount);
    }

    @Transactional(readOnly = true)
    public ValidateDiscountResponse validateCode(UUID tenantId, String code, BigDecimal orderAmount) {
        var discountOpt = discountRepository.findByCodeAndTenantId(code, tenantId);

        if (discountOpt.isEmpty()) {
            return new ValidateDiscountResponse(false, "Discount code not found", null, null, null, null);
        }

        Discount discount = discountOpt.get();

        if (!discount.isActive()) {
            return new ValidateDiscountResponse(false, "Discount is not active", discount.getName(),
                    discount.getType().name(), discount.getValue(), null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (discount.getStartsAt() != null && now.isBefore(discount.getStartsAt())) {
            return new ValidateDiscountResponse(false, "Discount has not started yet", discount.getName(),
                    discount.getType().name(), discount.getValue(), null);
        }

        if (discount.getEndsAt() != null && now.isAfter(discount.getEndsAt())) {
            return new ValidateDiscountResponse(false, "Discount has expired", discount.getName(),
                    discount.getType().name(), discount.getValue(), null);
        }

        if (discount.getUsageLimit() != null && discount.getUsageCount() >= discount.getUsageLimit()) {
            return new ValidateDiscountResponse(false, "Discount usage limit reached", discount.getName(),
                    discount.getType().name(), discount.getValue(), null);
        }

        if (discount.getMinOrderAmount() != null && orderAmount != null
                && orderAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            return new ValidateDiscountResponse(false,
                    "Order amount below minimum of " + discount.getMinOrderAmount(),
                    discount.getName(), discount.getType().name(), discount.getValue(), null);
        }

        BigDecimal discountAmount = calculateDiscountAmount(discount, orderAmount);

        return new ValidateDiscountResponse(true, "Discount is valid", discount.getName(),
                discount.getType().name(), discount.getValue(), discountAmount);
    }

    public void incrementUsage(UUID id, UUID tenantId) {
        Discount discount = discountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        discount.setUsageCount(discount.getUsageCount() + 1);
        discountRepository.save(discount);
        log.info("Incremented usage for discount '{}' (id={}) to {} for tenant {}",
                discount.getName(), id, discount.getUsageCount(), tenantId);
    }

    private void validateDiscountValues(DiscountType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than 0");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        }
    }

    private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal orderAmount) {
        if (orderAmount == null) {
            return BigDecimal.ZERO;
        }
        if (discount.getType() == DiscountType.PERCENTAGE) {
            return orderAmount.multiply(discount.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            return discount.getValue().min(orderAmount);
        }
    }

    private DiscountDto toDto(Discount discount) {
        List<UUID> productIds = discount.getDiscountProducts() != null
                ? discount.getDiscountProducts().stream().map(DiscountProduct::getProductId).toList()
                : Collections.emptyList();
        List<UUID> categoryIds = discount.getDiscountCategories() != null
                ? discount.getDiscountCategories().stream().map(DiscountCategory::getCategoryId).toList()
                : Collections.emptyList();

        return new DiscountDto(
                discount.getId(),
                discount.getName(),
                discount.getCode(),
                discount.getType().name(),
                discount.getValue(),
                discount.getMinOrderAmount(),
                discount.getUsageLimit(),
                discount.getUsageCount(),
                discount.getStartsAt(),
                discount.getEndsAt(),
                discount.isActive(),
                productIds,
                categoryIds,
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }

    private DiscountListDto toListDto(Discount discount) {
        return new DiscountListDto(
                discount.getId(),
                discount.getName(),
                discount.getCode(),
                discount.getType().name(),
                discount.getValue(),
                discount.getUsageLimit(),
                discount.getUsageCount(),
                discount.getStartsAt(),
                discount.getEndsAt(),
                discount.isActive(),
                discount.getCreatedAt()
        );
    }
}
