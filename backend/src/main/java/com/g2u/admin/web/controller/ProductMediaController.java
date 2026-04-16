package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MediaService;
import com.g2u.admin.web.dto.LinkMediaRequest;
import com.g2u.admin.web.dto.ProductMediaDetailDto;
import com.g2u.admin.web.dto.ReorderMediaRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/media")
public class ProductMediaController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductMediaController.class);

    private final MediaService mediaService;

    public ProductMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public List<ProductMediaDetailDto> getProductMedia(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId) {
        LOG.debug("REST request to get media for product {} tenant {}", productId, principal.tenantId());
        return mediaService.getProductMedia(productId, principal.tenantId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ProductMediaDetailDto> linkMedia(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody LinkMediaRequest request) {
        LOG.debug("REST request to link media {} to product {} tenant {}",
                request.mediaId(), productId, principal.tenantId());
        ProductMediaDetailDto dto = mediaService.addMediaToProduct(
                productId, request.mediaId(), principal.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> unlinkMedia(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID mediaId) {
        LOG.debug("REST request to unlink media {} from product {} tenant {}",
                mediaId, productId, principal.tenantId());
        mediaService.removeMediaFromProduct(productId, mediaId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> reorder(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ReorderMediaRequest request) {
        LOG.debug("REST request to reorder media for product {} tenant {}", productId, principal.tenantId());
        mediaService.reorderProductMedia(productId, principal.tenantId(), request.mediaIds());
        return ResponseEntity.ok().build();
    }
}
