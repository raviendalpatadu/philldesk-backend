package com.philldesk.philldeskbackend.dto;

import com.philldesk.philldeskbackend.entity.Prescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadyForPickupPrescriptionDTO {
    
    private Long id;
    private String prescriptionNumber;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String doctorName;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String status;
    
    // Bill information (flattened to avoid nesting)
    private Long billId;
    private String billNumber;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String paymentType;
    
    // Medicine items (simplified)
    private List<PrescriptionItemDTO> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionItemDTO {
        private String medicineName;
        private String strength;
        private String manufacturer;
        private String batchNumber;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String dosage;
        private String instructions;
    }
    
    // Static factory method to create DTO from Prescription entity
    public static ReadyForPickupPrescriptionDTO fromPrescription(Prescription prescription) {
        ReadyForPickupPrescriptionDTO dto = new ReadyForPickupPrescriptionDTO();
        
        // Basic prescription info
        dto.setId(prescription.getId());
        dto.setPrescriptionNumber(prescription.getPrescriptionNumber());
        dto.setCustomerName(prescription.getCustomer().getFirstName() + " " + prescription.getCustomer().getLastName());
        dto.setCustomerPhone(prescription.getCustomer().getPhone());
        dto.setCustomerEmail(prescription.getCustomer().getEmail());
        dto.setDoctorName(prescription.getDoctorName());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setApprovedAt(prescription.getApprovedAt());
        dto.setStatus(prescription.getStatus().toString());
        
        // Bill info (flattened)
        if (prescription.getBill() != null) {
            dto.setBillId(prescription.getBill().getId());
            dto.setBillNumber(prescription.getBill().getBillNumber());
            dto.setTotalAmount(prescription.getBill().getTotalAmount());
            dto.setPaymentStatus(prescription.getBill().getPaymentStatus().toString());
            dto.setPaymentType(prescription.getBill().getPaymentType().toString());
        }
        
        // Medicine items (simplified)
        if (prescription.getPrescriptionItems() != null) {
            List<PrescriptionItemDTO> itemDTOs = prescription.getPrescriptionItems().stream()
                .map(item -> new PrescriptionItemDTO(
                    item.getMedicine().getName(),
                    item.getMedicine().getStrength(),
                    item.getMedicine().getManufacturer(),
                    item.getMedicine().getBatchNumber(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice(),
                    item.getDosage(),
                    item.getInstructions()
                ))
                .toList();
            dto.setItems(itemDTOs);
        }
        
        return dto;
    }
}
