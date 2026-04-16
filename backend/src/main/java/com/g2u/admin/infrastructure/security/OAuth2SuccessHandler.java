package com.g2u.admin.infrastructure.security;

import com.g2u.admin.domain.auth.RefreshToken;
import com.g2u.admin.domain.auth.RefreshTokenRepository;
import com.g2u.admin.domain.auth.TokenHashUtil;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.domain.user.Role;
import com.g2u.admin.domain.user.User;
import com.g2u.admin.domain.user.UserRepository;
import com.g2u.admin.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                TenantRepository tenantRepository,
                                JwtTokenProvider jwtTokenProvider,
                                RefreshTokenRepository refreshTokenRepository,
                                NotificationService notificationService,
                                @Value("${app.cors.allowed-origins}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl.split(",")[0].trim();
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, picture, providerId));

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), tenantId, user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store refresh token hash in DB
        Instant expiresAt = jwtTokenProvider.parseToken(refreshToken).getExpiration().toInstant();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHashUtil.hashToken(refreshToken))
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private User createNewUser(String email, String name, String picture, String providerId) {
        Tenant defaultTenant = tenantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenant exists. Create a tenant first."));

        boolean isFirstUser = userRepository.count() == 0;

        Role assignedRole = isFirstUser ? Role.SUPER_ADMIN : Role.TENANT_VIEWER;

        User user = User.builder()
                .email(email)
                .name(name != null ? name : email)
                .avatarUrl(picture)
                .oauthProvider("google")
                .oauthProviderId(providerId)
                .tenant(defaultTenant)
                .role(assignedRole)
                .active(true)
                .build();
        user = userRepository.save(user);
        log.info("Created new OAuth2 user '{}' with role {} in tenant {}", email, assignedRole, defaultTenant.getId());

        // Notify tenant admins about the new user
        notificationService.createNotification(
                defaultTenant.getId(),
                null,
                NotificationType.NEW_USER,
                "New user registered: " + (name != null ? name : email),
                "User " + email + " has joined with role " + assignedRole.name()
        );

        return user;
    }
}
