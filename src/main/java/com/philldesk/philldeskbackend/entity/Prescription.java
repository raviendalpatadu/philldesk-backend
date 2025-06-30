package com.philldesk.philldeskbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "prescription_number", unique = true, nullable = false, length = 50)
    private String prescriptionNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacist_id")
    private User pharmacist;
    
    @Column(name = "doctor_name", length = 100)
    private String doctorName;
    
    @Column(name = "doctor_license", length = 50)
    private String doctorLicense;
    
    @Column(name = "prescription_date")
    private LocalDateTime prescriptionDate;
    
    @Column(name = "file_url", length = 500)
    private String fileUrl; // Google Drive URL
    
    @Column(name = "file_name", length = 200)
    private String fileName;
    
    @Column(name = "file_type", length = 20)
    private String fileType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.PENDING;
    
    @Column(name = "notes", length = 1000)
    private String notes; // Pharmacist notes
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private Set<PrescriptionItem> prescriptionItems;
    
    @OneToOne(mappedBy = "prescription", cascade = CascadeType.ALL)
    private Bill bill;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    public enum PrescriptionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        DISPENSED,
        COMPLETED
    }
}
