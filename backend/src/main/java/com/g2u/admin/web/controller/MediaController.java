package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.MediaService;
import com.g2u.admin.service.storage.FileStorageService;
import com.g2u.admin.web.dto.MediaDto;
import com.g2u.admin.web.dto.UpdateMediaRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Logger LOG = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;
    private final FileStorageService fileStorageService;

    public MediaController(MediaService mediaService, FileStorageService fileStorageService) {
        this.mediaService = mediaService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<MediaDto> upload(
            @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal principal) {
        LOG.debug("REST request to upload media for tenant {}", principal.tenantId());
        MediaDto dto = mediaService.upload(principal.tenantId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public List<MediaDto> listMedia(@CurrentUser UserPrincipal principal) {
        LOG.debug("REST request to list media for tenant {}", principal.tenantId());
        return mediaService.listMedia(principal.tenantId());
    }

    @GetMapping("/{id}")
    public MediaDto getMedia(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        LOG.debug("REST request to get media {} for tenant {}", id, principal.tenantId());
        return mediaService.getMedia(id, principal.tenantId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public MediaDto update(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMediaRequest request) {
        LOG.debug("REST request to update media {} for tenant {}", id, principal.tenantId());
        return mediaService.updateAltText(id, principal.tenantId(), request.altText());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> delete(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        LOG.debug("REST request to delete media {} for tenant {}", id, principal.tenantId());
        mediaService.delete(id, principal.tenantId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Serves uploaded media files. This endpoint is publicly accessible
     * (configured in SecurityConfig) so images can be embedded in storefronts.
     */
    @GetMapping("/{tenantId}/{filename}")
    public ResponseEntity<Resource> serve(
            @PathVariable String tenantId,
            @PathVariable String filename) {
        LOG.debug("REST request to serve file {} for tenant {}", filename, tenantId);
        String path = tenantId + "/" + filename;
        Resource resource = fileStorageService.load(path);

        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource);
    }
}
