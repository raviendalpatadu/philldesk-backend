package com.philldesk.philldeskbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "prescription_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(length = 500)
    private String dosage; // e.g., "Take 1 tablet twice daily after meals"
    
    @Column(length = 100)
    private String frequency; // e.g., "Twice daily", "As needed"
    
    @Column(length = 200)
    private String instructions; // Additional instructions
    
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice; // Price at the time of prescription
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice; // quantity * unitPrice
    
    @Column(name = "is_dispensed", nullable = false)
    private Boolean isDispensed = false;
    
    // Calculate total price before saving
    @PrePersist
    @PreUpdate
    private void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
