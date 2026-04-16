package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.DiscountService;
import com.g2u.admin.web.dto.CreateDiscountRequest;
import com.g2u.admin.web.dto.DiscountDto;
import com.g2u.admin.web.dto.DiscountListDto;
import com.g2u.admin.web.dto.ToggleActiveRequest;
import com.g2u.admin.web.dto.UpdateDiscountRequest;
import com.g2u.admin.web.dto.ValidateDiscountResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public Page<DiscountListDto> listDiscounts(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return discountService.getDiscounts(principal.tenantId(), pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<DiscountDto> createDiscount(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateDiscountRequest request) {
        DiscountDto discount = discountService.createDiscount(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(discount);
    }

    @GetMapping("/{id}")
    public DiscountDto getDiscount(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        return discountService.getDiscount(id, principal.tenantId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public DiscountDto updateDiscount(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiscountRequest request) {
        return discountService.updateDiscount(id, principal.tenantId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDiscount(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        discountService.deleteDiscount(id, principal.tenantId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public DiscountDto toggleActive(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ToggleActiveRequest request) {
        return discountService.toggleActive(id, principal.tenantId(), request.active());
    }

    @PostMapping("/validate")
    public ValidateDiscountResponse validateCode(
            @CurrentUser UserPrincipal principal,
            @RequestParam String code,
            @RequestParam(required = false) BigDecimal orderAmount) {
        return discountService.validateCode(principal.tenantId(), code, orderAmount);
    }
}
