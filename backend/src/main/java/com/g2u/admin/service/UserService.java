package com.g2u.admin.service;

import com.g2u.admin.domain.user.Role;
import com.g2u.admin.domain.user.User;
import com.g2u.admin.domain.user.UserRepository;
import com.g2u.admin.web.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTenant(UUID tenantId) {
        return userRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    public UserDto updateRole(UUID tenantId, UUID callerId, String callerRoleName, UUID userId, String roleName) {
        if (callerId.equals(userId)) {
            throw new IllegalArgumentException("Cannot change your own role");
        }

        Role callerRole = Role.valueOf(callerRoleName.toUpperCase());
        Role targetRole = Role.valueOf(roleName.toUpperCase());

        if (callerRole != Role.SUPER_ADMIN &&
                (targetRole == Role.SUPER_ADMIN)) {
            throw new IllegalArgumentException("TENANT_ADMIN can only assign TENANT_ADMIN or TENANT_VIEWER roles");
        }

        User user = userRepository.findById(userId)
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("User not found in tenant"));

        // TENANT_ADMIN cannot change the role of a SUPER_ADMIN
        if (callerRole != Role.SUPER_ADMIN && user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify a SUPER_ADMIN user's role");
        }

        String previousRole = user.getRole().name();
        user.setRole(targetRole);
        user = userRepository.save(user);
        log.info("Updated role for user {} from {} to {} in tenant {}", userId, previousRole, targetRole.name(), tenantId);

        auditService.log(tenantId, callerId, "UPDATE", "USER", userId,
                Map.of("field", "role", "from", previousRole, "to", targetRole.name()));

        return toDto(user);
    }

    public UserDto toggleActive(UUID tenantId, UUID callerId, UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("User not found in tenant"));

        boolean previousActive = user.isActive();
        user.setActive(!previousActive);
        user = userRepository.save(user);
        log.info("Toggled active status for user {} to {} in tenant {}", userId, user.isActive(), tenantId);

        auditService.log(tenantId, callerId, "UPDATE", "USER", userId,
                Map.of("field", "active", "from", previousActive, "to", user.isActive()));

        return toDto(user);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.isActive(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getCreatedAt()
        );
    }
}
