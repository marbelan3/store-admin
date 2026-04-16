package com.g2u.admin.service;

import com.g2u.admin.domain.notification.Notification;
import com.g2u.admin.domain.notification.NotificationRepository;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.web.dto.NotificationDto;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(UUID tenantId, UUID userId, NotificationType type,
                                           String title, String message) {
        LOG.debug("Creating notification: type={}, title={}, tenant={}", type, title, tenantId);

        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(UUID tenantId, Pageable pageable) {
        return notificationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID tenantId) {
        return notificationRepository.countByTenantIdAndReadFalse(tenantId);
    }

    @Transactional
    public NotificationDto markAsRead(UUID id, UUID tenantId) {
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return toDto(notification);
    }

    @Transactional
    public int markAllAsRead(UUID tenantId) {
        return notificationRepository.markAllAsRead(tenantId);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getTenantId(),
                n.getUserId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
