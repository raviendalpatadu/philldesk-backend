package com.philldesk.philldeskbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"prescription", "customer", "billItems"})
@EqualsAndHashCode(exclude = {"billItems"})
public class Bill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bill_number", unique = true, nullable = false, length = 50)
    private String billNumber;
    
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prescription_id")
    @JsonBackReference
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"prescriptions", "handledPrescriptions", "notifications", "password"})
    private User customer;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pharmacist_id", nullable = false)
    @JsonIgnoreProperties({"prescriptions", "handledPrescriptions", "notifications", "password"})
    private User pharmacist;
    
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private Set<BillItem> billItems;
    
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType = PaymentType.PAY_ON_PICKUP;
    
    @Column(length = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    public enum PaymentStatus {
        PENDING,
        PAID,
        PARTIALLY_PAID,
        CANCELLED
    }
    
    public enum PaymentMethod {
        CASH,
        CARD,
        BANK_TRANSFER,
        ONLINE,
        OTHER
    }
    
    public enum PaymentType {
        ONLINE,
        PAY_ON_PICKUP
    }
    
    // Calculate total amount before saving
    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        BigDecimal discountAmount = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal taxAmount = tax != null ? tax : BigDecimal.ZERO;
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        
        totalAmount = sub.subtract(discountAmount).add(taxAmount);
    }
}
