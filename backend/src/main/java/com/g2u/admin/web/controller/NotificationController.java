package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.NotificationService;
import com.g2u.admin.web.dto.NotificationDto;
import com.g2u.admin.web.dto.UnreadCountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationDto> listNotifications(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        LOG.debug("REST request to get notifications for tenant: {}", principal.tenantId());
        return notificationService.getNotifications(principal.tenantId(), pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountDto getUnreadCount(@CurrentUser UserPrincipal principal) {
        LOG.debug("REST request to get unread notification count for tenant: {}", principal.tenantId());
        long count = notificationService.getUnreadCount(principal.tenantId());
        return new UnreadCountDto(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {
        LOG.debug("REST request to mark notification as read: {}", id);
        NotificationDto dto = notificationService.markAsRead(id, principal.tenantId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@CurrentUser UserPrincipal principal) {
        LOG.debug("REST request to mark all notifications as read for tenant: {}", principal.tenantId());
        int updated = notificationService.markAllAsRead(principal.tenantId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
