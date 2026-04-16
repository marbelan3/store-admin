package com.g2u.admin.web.controller;

import com.g2u.admin.domain.auth.RefreshToken;
import com.g2u.admin.domain.auth.RefreshTokenRepository;
import com.g2u.admin.domain.auth.TokenHashUtil;
import com.g2u.admin.domain.user.User;
import com.g2u.admin.domain.user.UserRepository;
import com.g2u.admin.infrastructure.security.JwtTokenProvider;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = userRepository.findById(principal.userId())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "role", user.getRole().name(),
                "tenantId", user.getTenant() != null ? user.getTenant().getId() : ""
        ));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        // Verify the token exists in DB (not revoked)
        String tokenHash = TokenHashUtil.hashToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);
        if (stored == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token revoked or not found"));
        }

        // Delete the old refresh token (rotation)
        refreshTokenRepository.delete(stored);

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null || !user.isActive()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found or inactive"));
        }

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), tenantId, user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store the new refresh token hash
        storeRefreshToken(user.getId(), newRefreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            refreshTokenRepository.deleteByUserId(principal.userId());
        }
        return ResponseEntity.ok().build();
    }

    private void storeRefreshToken(UUID userId, String rawToken) {
        Instant expiresAt = jwtTokenProvider.parseToken(rawToken).getExpiration().toInstant();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenHashUtil.hashToken(rawToken))
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);
    }
}
