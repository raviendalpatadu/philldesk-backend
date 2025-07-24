package com.philldesk.philldeskbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BillProjection {
    Long getId();
    String getBillNumber();
    BigDecimal getSubtotal();
    BigDecimal getDiscount();
    BigDecimal getTax();
    BigDecimal getTotalAmount();
    String getPaymentStatus();
    String getPaymentMethod();
    String getPaymentType();
    String getNotes();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getPaidAt();
    
    // Customer info
    Long getCustomerId();
    String getCustomerFirstName();
    String getCustomerLastName();
    
    // Prescription info
    Long getPrescriptionId();
    String getPrescriptionNumber();
}
