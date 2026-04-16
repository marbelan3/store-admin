package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.CustomerService;
import com.g2u.admin.web.dto.CreateCustomerAddressRequest;
import com.g2u.admin.web.dto.CreateCustomerRequest;
import com.g2u.admin.web.dto.CustomerAddressDto;
import com.g2u.admin.web.dto.CustomerDto;
import com.g2u.admin.web.dto.CustomerListDto;
import com.g2u.admin.web.dto.UpdateCustomerRequest;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Page<CustomerListDto> listCustomers(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return customerService.getCustomers(principal.tenantId(), search, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<CustomerDto> createCustomer(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerDto customer = customerService.createCustomer(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    @GetMapping("/{id}")
    public CustomerDto getCustomer(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return customerService.getCustomer(id, principal.tenantId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public CustomerDto updateCustomer(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.updateCustomer(id, principal.tenantId(), principal.userId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        customerService.deleteCustomer(id, principal.tenantId(), principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<CustomerAddressDto> addAddress(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCustomerAddressRequest request) {
        CustomerAddressDto address = customerService.addAddress(id, principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public CustomerAddressDto updateAddress(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID addressId,
            @Valid @RequestBody CreateCustomerAddressRequest request) {
        return customerService.updateAddress(id, addressId, principal.tenantId(), request);
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteAddress(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID addressId) {
        customerService.deleteAddress(id, addressId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }
}
