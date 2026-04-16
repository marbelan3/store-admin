package com.g2u.admin.infrastructure.security;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        String email,
        String role,
        UUID tenantId
) {
}
