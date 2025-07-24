package com.philldesk.philldeskbackend.dto;

import java.math.BigDecimal;

public class PrescriptionItemDTO {
    private Long medicineId;
    private Integer quantity;
    private String dosage;
    private String frequency;
    private String instructions;
    private BigDecimal unitPrice;

    public PrescriptionItemDTO() {}

    public PrescriptionItemDTO(Long medicineId, Integer quantity, String dosage, String frequency, String instructions, BigDecimal unitPrice) {
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.dosage = dosage;
        this.frequency = frequency;
        this.instructions = instructions;
        this.unitPrice = unitPrice;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
