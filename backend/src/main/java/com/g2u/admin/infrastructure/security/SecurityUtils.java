package com.g2u.admin.infrastructure.security;

import com.g2u.admin.domain.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static UUID getCurrentUserId() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.userId() : null;
    }

    public static UUID getCurrentTenantId() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.tenantId() : null;
    }

    public static boolean isSuperAdmin() {
        UserPrincipal principal = getCurrentUser();
        return principal != null && Role.SUPER_ADMIN.name().equals(principal.role());
    }

    public static boolean isTenantAdmin() {
        UserPrincipal principal = getCurrentUser();
        return principal != null && Role.TENANT_ADMIN.name().equals(principal.role());
    }
}
