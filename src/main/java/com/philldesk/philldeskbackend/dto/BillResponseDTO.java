package com.philldesk.philldeskbackend.dto;

import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.BillItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillResponseDTO {
    private Long id;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private Long prescriptionId;
    private String prescriptionNumber;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentType;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private List<BillItemResponseDTO> billItems;

    public static BillResponseDTO fromEntity(Bill bill) {
        if (bill == null) {
            return null;
        }
        
        BillResponseDTO dto = new BillResponseDTO();
        dto.setId(bill.getId());
        dto.setBillNumber(bill.getBillNumber());
        
        // Safe access to customer
        if (bill.getCustomer() != null) {
            dto.setCustomerId(bill.getCustomer().getId());
            dto.setCustomerName(bill.getCustomer().getFirstName() + " " + bill.getCustomer().getLastName());
        }
        
        // Safe access to prescription
        if (bill.getPrescription() != null) {
            dto.setPrescriptionId(bill.getPrescription().getId());
            dto.setPrescriptionNumber(bill.getPrescription().getPrescriptionNumber());
        }
        
        dto.setSubtotal(bill.getSubtotal());
        dto.setDiscount(bill.getDiscount());
        dto.setTax(bill.getTax());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setPaymentStatus(bill.getPaymentStatus() != null ? bill.getPaymentStatus().name() : null);
        dto.setPaymentMethod(bill.getPaymentMethod() != null ? bill.getPaymentMethod().name() : null);
        dto.setPaymentType(bill.getPaymentType() != null ? bill.getPaymentType().name() : null);
        dto.setNotes(bill.getNotes());
        dto.setCreatedAt(bill.getCreatedAt());
        dto.setUpdatedAt(bill.getUpdatedAt());
        dto.setPaidAt(bill.getPaidAt());
        
        // Safe conversion of bill items
        if (bill.getBillItems() != null) {
            dto.setBillItems(bill.getBillItems().stream()
                .map(BillItemResponseDTO::fromEntity)
                .toList());
        }
        
        return dto;
    }

    @Data
    public static class BillItemResponseDTO {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static BillItemResponseDTO fromEntity(BillItem billItem) {
            if (billItem == null) {
                return null;
            }
            
            BillItemResponseDTO dto = new BillItemResponseDTO();
            dto.setId(billItem.getId());
            
            // Safe access to medicine
            if (billItem.getMedicine() != null) {
                dto.setMedicineId(billItem.getMedicine().getId());
                dto.setMedicineName(billItem.getMedicine().getName());
            }
            
            dto.setQuantity(billItem.getQuantity());
            dto.setUnitPrice(billItem.getUnitPrice());
            dto.setTotalPrice(billItem.getTotalPrice());
            
            return dto;
        }
    }
}
