package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.TenantService;
import com.g2u.admin.web.dto.TenantDto;
import com.g2u.admin.web.dto.UpdateTenantRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public TenantDto getTenant(@CurrentUser UserPrincipal principal) {
        return tenantService.getTenant(principal.tenantId());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public TenantDto updateTenant(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.updateTenant(principal.tenantId(), request);
    }
}
