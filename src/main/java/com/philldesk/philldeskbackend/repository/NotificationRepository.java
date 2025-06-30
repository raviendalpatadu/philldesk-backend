package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.entity.Notification;
import com.philldesk.philldeskbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUser(User user);
    
    List<Notification> findByUserAndIsReadFalse(User user);
    
    List<Notification> findByNotificationType(Notification.NotificationType notificationType);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countByUserIdAndIsReadFalse(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.notificationType = :type AND n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByTypeAndDateRange(@Param("type") Notification.NotificationType type,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT n FROM Notification n WHERE n.priority = :priority AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByPriority(@Param("priority") Notification.Priority priority);
    
    @Query("SELECT n FROM Notification n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType")
    List<Notification> findByReference(@Param("referenceId") Long referenceId, 
                                     @Param("referenceType") String referenceType);
    
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate AND n.isRead = true")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
