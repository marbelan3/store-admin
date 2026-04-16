package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.WebhookService;
import com.g2u.admin.web.dto.CreateWebhookRequest;
import com.g2u.admin.web.dto.UpdateWebhookRequest;
import com.g2u.admin.web.dto.WebhookDeliveryDto;
import com.g2u.admin.web.dto.WebhookEndpointDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class WebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public Page<WebhookEndpointDto> listWebhooks(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        LOG.debug("REST request to list webhooks for tenant: {}", principal.tenantId());
        return webhookService.getWebhooks(principal.tenantId(), pageable);
    }

    @PostMapping
    public ResponseEntity<WebhookEndpointDto> createWebhook(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateWebhookRequest request) {
        LOG.debug("REST request to create webhook for tenant: {}", principal.tenantId());
        WebhookEndpointDto dto = webhookService.createWebhook(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public WebhookEndpointDto updateWebhook(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookRequest request) {
        LOG.debug("REST request to update webhook: {}", id);
        return webhookService.updateWebhook(principal.tenantId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        LOG.debug("REST request to delete webhook: {}", id);
        webhookService.deleteWebhook(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    public Page<WebhookDeliveryDto> getDeliveries(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        LOG.debug("REST request to get deliveries for webhook: {}", id);
        return webhookService.getDeliveries(principal.tenantId(), id, pageable);
    }
}
