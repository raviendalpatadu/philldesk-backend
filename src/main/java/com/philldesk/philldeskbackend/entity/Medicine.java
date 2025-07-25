package com.philldesk.philldeskbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "medicines", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "strength", "dosage_form", "manufacturer"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"prescriptionItems", "billItems"})
@EqualsAndHashCode(exclude = {"prescriptionItems", "billItems"})
public class Medicine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(name = "generic_name", length = 200)
    private String genericName;
    
    @Column(length = 100)
    private String manufacturer;
    
    @Column(length = 50)
    private String category;
    
    @Column(name = "dosage_form", length = 50)
    private String dosageForm; // Tablet, Capsule, Syrup, etc.
    
    @Column(length = 50)
    private String strength; // 500mg, 10ml, etc.
    
    @Column(nullable = false)
    private Integer quantity = 0;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "batch_number", length = 50)
    private String batchNumber;
    
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 10; // Minimum stock threshold
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "is_prescription_required", nullable = false)
    private Boolean isPrescriptionRequired = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<PrescriptionItem> prescriptionItems;
    
    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<BillItem> billItems;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper method to check if medicine is low on stock
    public boolean isLowStock() {
        return this.quantity <= this.reorderLevel;
    }
    
    // Helper method to check if medicine is expired or expiring soon
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
    
    public boolean isExpiringSoon(int days) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(days));
    }
}
