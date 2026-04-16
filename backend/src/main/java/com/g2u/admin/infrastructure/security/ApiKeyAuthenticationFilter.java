package com.g2u.admin.infrastructure.security;

import com.g2u.admin.domain.auth.ApiKey;
import com.g2u.admin.domain.auth.ApiKeyRepository;
import com.g2u.admin.domain.auth.TokenHashUtil;
import com.g2u.admin.infrastructure.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String STOREFRONT_PATH = "/api/v1/storefront/";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(STOREFRONT_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKeyRaw = request.getHeader(API_KEY_HEADER);

        if (apiKeyRaw == null || apiKeyRaw.isBlank()) {
            log.debug("Storefront request rejected: missing API key header for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
            return;
        }

        String keyHash = TokenHashUtil.hashToken(apiKeyRaw);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null);

        if (apiKey == null || !apiKey.isActive()) {
            log.debug("Storefront request rejected: invalid or inactive API key for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or inactive API key\"}");
            return;
        }

        log.debug("API key authenticated: prefix={}, tenant={}", apiKey.getKeyPrefix(), apiKey.getTenantId());

        // Set tenant context
        TenantContext.setCurrentTenantId(apiKey.getTenantId());

        // Update last_used_at asynchronously (fire and forget, best effort)
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        // Create authentication with API_CLIENT role
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        UserPrincipal principal = new UserPrincipal(apiKey.getId(), "api-key:" + apiKey.getKeyPrefix(),
                "API_CLIENT", apiKey.getTenantId());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
