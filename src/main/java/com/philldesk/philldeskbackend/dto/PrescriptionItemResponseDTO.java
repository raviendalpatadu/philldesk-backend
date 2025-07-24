package com.philldesk.philldeskbackend.dto;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrescriptionItemResponseDTO {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String medicineStrength;
    private String dosageForm;
    private Integer quantity;
    private String dosage;
    private String frequency;
    private String instructions;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private boolean isDispensed;

    public static PrescriptionItemResponseDTO fromEntity(PrescriptionItem item) {
        PrescriptionItemResponseDTO dto = new PrescriptionItemResponseDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setDosage(item.getDosage());
        dto.setFrequency(item.getFrequency());
        dto.setInstructions(item.getInstructions());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setDispensed(Boolean.TRUE.equals(item.getIsDispensed()));
        
        // Safely access medicine info
        if (item.getMedicine() != null) {
            dto.setMedicineId(item.getMedicine().getId());
            dto.setMedicineName(item.getMedicine().getName());
            dto.setMedicineStrength(item.getMedicine().getStrength());
            dto.setDosageForm(item.getMedicine().getDosageForm());
        }
        
        return dto;
    }
}
