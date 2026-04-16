package com.g2u.admin.service;

import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.TenantDto;
import com.g2u.admin.web.dto.UpdateTenantRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public TenantDto getTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        return toDto(tenant);
    }

    public TenantDto updateTenant(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (request.name() != null) tenant.setName(request.name());
        if (request.logoUrl() != null) tenant.setLogoUrl(request.logoUrl());
        if (request.primaryColor() != null) tenant.setPrimaryColor(request.primaryColor());
        if (request.taxRate() != null) tenant.setTaxRate(request.taxRate());
        if (request.businessEmail() != null) tenant.setBusinessEmail(request.businessEmail());
        if (request.businessPhone() != null) tenant.setBusinessPhone(request.businessPhone());
        if (request.businessAddress() != null) tenant.setBusinessAddress(request.businessAddress());
        if (request.website() != null) tenant.setWebsite(request.website());
        if (request.settings() != null) tenant.setSettings(request.settings());

        tenant = tenantRepository.save(tenant);
        return toDto(tenant);
    }

    private TenantDto toDto(Tenant tenant) {
        return new TenantDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getLogoUrl(),
                tenant.getPrimaryColor(),
                tenant.getTaxRate(),
                tenant.getBusinessEmail(),
                tenant.getBusinessPhone(),
                tenant.getBusinessAddress(),
                tenant.getWebsite(),
                tenant.getSettings(),
                tenant.isActive()
        );
    }
}
