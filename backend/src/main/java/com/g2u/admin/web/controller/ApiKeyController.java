package com.g2u.admin.web.controller;

import com.g2u.admin.domain.auth.ApiKey;
import com.g2u.admin.domain.auth.ApiKeyRepository;
import com.g2u.admin.domain.auth.TokenHashUtil;
import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class ApiKeyController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyController(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list(@CurrentUser UserPrincipal principal) {
        List<ApiKeyResponse> keys = apiKeyRepository.findByTenantId(principal.tenantId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(keys);
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(
            @RequestBody CreateApiKeyRequest request,
            @CurrentUser UserPrincipal principal) {

        // Generate raw key: sk_ + 32 random hex chars
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawKey = "sk_" + HexFormat.of().formatHex(randomBytes);

        String keyHash = TokenHashUtil.hashToken(rawKey);
        String keyPrefix = rawKey.substring(0, 8);

        ApiKey apiKey = ApiKey.builder()
                .tenantId(principal.tenantId())
                .name(request.name())
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .active(true)
                .build();
        apiKeyRepository.save(apiKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiKeyCreatedResponse(
                apiKey.getId(), apiKey.getName(), rawKey, keyPrefix, apiKey.getCreatedAt()
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> toggleActive(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        ApiKey apiKey = apiKeyRepository.findById(id)
                .filter(k -> k.getTenantId().equals(principal.tenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setActive(!apiKey.isActive());
        apiKeyRepository.save(apiKey);
        return ResponseEntity.ok(toResponse(apiKey));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        ApiKey apiKey = apiKeyRepository.findById(id)
                .filter(k -> k.getTenantId().equals(principal.tenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKeyRepository.delete(apiKey);
        return ResponseEntity.noContent().build();
    }

    private ApiKeyResponse toResponse(ApiKey k) {
        return new ApiKeyResponse(
                k.getId(), k.getName(), k.getKeyPrefix(),
                k.isActive(), k.getCreatedAt(), k.getLastUsedAt()
        );
    }

    public record CreateApiKeyRequest(String name) {}

    public record ApiKeyResponse(
            UUID id, String name, String keyPrefix,
            boolean active,
            Instant createdAt, Instant lastUsedAt
    ) {}

    public record ApiKeyCreatedResponse(
            UUID id, String name, String rawKey, String keyPrefix,
            Instant createdAt
    ) {}
}
