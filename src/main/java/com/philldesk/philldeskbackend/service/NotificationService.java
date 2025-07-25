package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Notification;
import com.philldesk.philldeskbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    List<Notification> getAllNotifications();
    Page<Notification> getAllNotifications(Pageable pageable);
    Optional<Notification> getNotificationById(Long id);
    List<Notification> getNotificationsByUser(User user);
    List<Notification> getNotificationsByUserId(Long userId);
    List<Notification> getUnreadNotificationsByUser(User user);
    List<Notification> getUnreadNotificationsByUserId(Long userId);
    List<Notification> getNotificationsByType(Notification.NotificationType type);
    Notification saveNotification(Notification notification);
    Notification updateNotification(Notification notification);
    void deleteNotification(Long id);
    void markAsRead(Long notificationId);
    void markAllAsReadForUser(Long userId);
    void createNotification(Long userId, String title, String message, Notification.NotificationType type);
    void createLowStockNotification(Long medicineId);
    void createExpiryAlertNotification(Long medicineId);
    void createPrescriptionNotification(Long prescriptionId, String message, Notification.NotificationType type);
    Long getUnreadCountForUser(Long userId);
}
