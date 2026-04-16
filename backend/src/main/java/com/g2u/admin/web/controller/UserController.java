package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.UserService;
import com.g2u.admin.web.dto.UpdateUserRoleRequest;
import com.g2u.admin.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> listUsers(@CurrentUser UserPrincipal principal) {
        return userService.getUsersByTenant(principal.tenantId());
    }

    @PatchMapping("/{id}/role")
    public UserDto updateRole(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return userService.updateRole(principal.tenantId(), principal.userId(), principal.role(), id, request.role());
    }

    @PatchMapping("/{id}/toggle-active")
    public UserDto toggleActive(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return userService.toggleActive(principal.tenantId(), principal.userId(), id);
    }
}
