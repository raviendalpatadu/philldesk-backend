package com.philldesk.philldeskbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"bill"})
@EqualsAndHashCode(exclude = {"bill"})
public class ShippingDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false, unique = true)
    @JsonBackReference
    private Bill bill;
    
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;
    
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;
    
    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;
    
    @Column(name = "address_line2", length = 200)
    private String addressLine2;
    
    @Column(name = "city", nullable = false, length = 50)
    private String city;
    
    @Column(name = "state_province", nullable = false, length = 50)
    private String stateProvince;
    
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    
    @Column(name = "country", nullable = false, length = 50)
    private String country;
    
    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;
    
    @Column(name = "preferred_delivery_time", length = 100)
    private String preferredDeliveryTime;
    
    @Column(name = "shipping_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShippingStatus shippingStatus = ShippingStatus.PENDING;
    
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
    
    @Column(name = "tracking_url", length = 255)
    private String trackingUrl;
    
    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;
    
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private java.math.BigDecimal deliveryFee;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ShippingStatus {
        PENDING,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        CONFIRMED,
        PICKED_UP,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        FAILED_DELIVERY,
        RETURNED
    }
}
