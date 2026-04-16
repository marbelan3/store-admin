package com.g2u.admin.service;

import com.g2u.admin.domain.notification.Notification;
import com.g2u.admin.domain.notification.NotificationRepository;
import com.g2u.admin.domain.notification.NotificationType;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Tenant A")
                .slug("tenant-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Tenant B")
                .slug("tenant-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createNotification_shouldAppearInList() {
        notificationService.createNotification(
                tenantAId, null, NotificationType.PRODUCT_CREATED,
                "New Product", "Product X was created");

        Page<NotificationDto> notifications = notificationService.getNotifications(
                tenantAId, PageRequest.of(0, 20));

        assertTrue(notifications.getTotalElements() >= 1);

        NotificationDto found = notifications.getContent().stream()
                .filter(n -> "New Product".equals(n.title()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created notification not found in list"));

        assertEquals("PRODUCT_CREATED", found.type());
        assertEquals("Product X was created", found.message());
        assertFalse(found.read());
        assertNotNull(found.id());
        assertNotNull(found.createdAt());
    }

    @Test
    void getUnreadCount_shouldReturnOneAfterCreatingUnreadNotification() {
        long countBefore = notificationService.getUnreadCount(tenantAId);

        notificationService.createNotification(
                tenantAId, null, NotificationType.LOW_STOCK,
                "Low Stock Alert", "Widget is running low");

        long countAfter = notificationService.getUnreadCount(tenantAId);
        assertEquals(countBefore + 1, countAfter);
    }

    @Test
    void markAsRead_shouldDecreaseUnreadCount() {
        Notification notification = notificationService.createNotification(
                tenantAId, null, NotificationType.SYSTEM,
                "System Notice", "Maintenance scheduled");

        long countBefore = notificationService.getUnreadCount(tenantAId);

        NotificationDto read = notificationService.markAsRead(notification.getId(), tenantAId);

        assertTrue(read.read());
        long countAfter = notificationService.getUnreadCount(tenantAId);
        assertEquals(countBefore - 1, countAfter);
    }

    @Test
    void markAllAsRead_shouldSetAllNotificationsToRead() {
        notificationService.createNotification(
                tenantAId, null, NotificationType.NEW_USER,
                "New User 1", "User joined");
        notificationService.createNotification(
                tenantAId, null, NotificationType.NEW_USER,
                "New User 2", "Another user joined");

        long unreadBefore = notificationService.getUnreadCount(tenantAId);
        assertTrue(unreadBefore >= 2, "Expected at least 2 unread notifications");

        int updated = notificationService.markAllAsRead(tenantAId);

        assertTrue(updated >= 2, "Expected at least 2 notifications marked as read");
        assertEquals(0, notificationService.getUnreadCount(tenantAId));
    }

    @Test
    void tenantIsolation_notificationFromTenantANotVisibleToTenantB() {
        notificationService.createNotification(
                tenantAId, null, NotificationType.IMPORT_COMPLETE,
                "Import Done", "CSV import finished");

        Page<NotificationDto> tenantBNotifications = notificationService.getNotifications(
                tenantBId, PageRequest.of(0, 20));

        boolean found = tenantBNotifications.getContent().stream()
                .anyMatch(n -> "Import Done".equals(n.title()));

        assertFalse(found, "Tenant B should not see Tenant A notifications");
    }

    @Test
    void pagination_shouldReturnCorrectPageResults() {
        for (int i = 1; i <= 5; i++) {
            notificationService.createNotification(
                    tenantAId, null, NotificationType.PRODUCT_UPDATED,
                    "Update " + i, "Product updated #" + i);
        }

        Page<NotificationDto> firstPage = notificationService.getNotifications(
                tenantAId, PageRequest.of(0, 3));
        Page<NotificationDto> secondPage = notificationService.getNotifications(
                tenantAId, PageRequest.of(1, 3));

        assertEquals(3, firstPage.getSize());
        assertEquals(3, firstPage.getNumberOfElements());
        assertTrue(firstPage.getTotalElements() >= 5);
        assertTrue(secondPage.getNumberOfElements() >= 2);
        assertFalse(firstPage.getContent().isEmpty());
        assertFalse(secondPage.getContent().isEmpty());

        // Verify no overlap between pages
        firstPage.getContent().forEach(fp ->
                secondPage.getContent().forEach(sp ->
                        assertNotEquals(fp.id(), sp.id(), "Pages should not contain duplicate notifications")));
    }
}
