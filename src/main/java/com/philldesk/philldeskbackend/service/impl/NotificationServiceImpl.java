package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.Notification;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.repository.NotificationRepository;
import com.philldesk.philldeskbackend.repository.UserRepository;
import com.philldesk.philldeskbackend.repository.MedicineRepository;
import com.philldesk.philldeskbackend.repository.PrescriptionRepository;
import com.philldesk.philldeskbackend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                 UserRepository userRepository,
                                 MedicineRepository medicineRepository,
                                 PrescriptionRepository prescriptionRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.medicineRepository = medicineRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsByUser(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByType(Notification.NotificationType type) {
        return notificationRepository.findByNotificationType(type);
    }

    @Override
    public Notification saveNotification(Notification notification) {
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public Notification updateNotification(Notification notification) {
        Optional<Notification> existingNotification = notificationRepository.findById(notification.getId());
        if (existingNotification.isPresent()) {
            notification.setCreatedAt(existingNotification.get().getCreatedAt());
        }
        return notificationRepository.save(notification);
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent()) {
            Notification existingNotification = notification.get();
            existingNotification.markAsRead();
            notificationRepository.save(existingNotification);
        }
    }

    @Override
    public void markAllAsReadForUser(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public void createNotification(Long userId, String title, String message, Notification.NotificationType type) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            Notification notification = new Notification();
            notification.setUser(user.get());
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(type);
            notification.setPriority(getPriorityForType(type));
            saveNotification(notification);
        }
    }

    @Override
    public void createLowStockNotification(Long medicineId) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        if (medicine.isPresent()) {
            Medicine med = medicine.get();
            String title = "Low Stock Alert";
            String message = String.format("Medicine '%s' is running low. Current stock: %d, Reorder level: %d", 
                    med.getName(), med.getQuantity(), med.getReorderLevel());
            
            // Notify all pharmacists and admins
            List<User> pharmacists = userRepository.findByRoleName(com.philldesk.philldeskbackend.entity.Role.RoleName.PHARMACIST);
            List<User> admins = userRepository.findByRoleName(com.philldesk.philldeskbackend.entity.Role.RoleName.ADMIN);
            
            for (User pharmacist : pharmacists) {
                Notification notification = new Notification();
                notification.setUser(pharmacist);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setNotificationType(Notification.NotificationType.LOW_STOCK);
                notification.setPriority(Notification.Priority.HIGH);
                notification.setReferenceId(medicineId);
                notification.setReferenceType("MEDICINE");
                saveNotification(notification);
            }
            
            for (User admin : admins) {
                Notification notification = new Notification();
                notification.setUser(admin);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setNotificationType(Notification.NotificationType.LOW_STOCK);
                notification.setPriority(Notification.Priority.HIGH);
                notification.setReferenceId(medicineId);
                notification.setReferenceType("MEDICINE");
                saveNotification(notification);
            }
        }
    }

    @Override
    public void createPrescriptionNotification(Long prescriptionId, String message, Notification.NotificationType type) {
        Optional<Prescription> prescription = prescriptionRepository.findById(prescriptionId);
        if (prescription.isPresent()) {
            Prescription p = prescription.get();
            String title = getNotificationTitle(type);
            
            Notification notification = new Notification();
            notification.setUser(p.getCustomer());
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(type);
            notification.setPriority(getPriorityForType(type));
            notification.setReferenceId(prescriptionId);
            notification.setReferenceType("PRESCRIPTION");
            saveNotification(notification);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private Notification.Priority getPriorityForType(Notification.NotificationType type) {
        return switch (type) {
            case LOW_STOCK, EXPIRY_ALERT -> Notification.Priority.HIGH;
            case PRESCRIPTION_REJECTED, SYSTEM_ALERT -> Notification.Priority.HIGH;
            case PRESCRIPTION_APPROVED, BILL_GENERATED -> Notification.Priority.MEDIUM;
            case PRESCRIPTION_UPLOADED, USER_REGISTRATION -> Notification.Priority.LOW;
        };
    }

    private String getNotificationTitle(Notification.NotificationType type) {
        return switch (type) {
            case LOW_STOCK -> "Low Stock Alert";
            case EXPIRY_ALERT -> "Medicine Expiry Alert";
            case PRESCRIPTION_UPLOADED -> "Prescription Uploaded";
            case PRESCRIPTION_APPROVED -> "Prescription Approved";
            case PRESCRIPTION_REJECTED -> "Prescription Rejected";
            case BILL_GENERATED -> "Bill Generated";
            case SYSTEM_ALERT -> "System Alert";
            case USER_REGISTRATION -> "Welcome!";
        };
    }
}
