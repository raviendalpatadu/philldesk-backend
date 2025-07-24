package com.philldesk.philldeskbackend.dto;

import com.philldesk.philldeskbackend.entity.Prescription;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionResponseDTO {
    private Long id;
    private String prescriptionNumber;
    private String doctorName;
    private String doctorLicense;
    private LocalDateTime prescriptionDate;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private String status;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    
    // Customer basic info (avoid full user object)
    private Long customerId;
    private String customerName;
    
    // Pharmacist basic info (avoid full user object)
    private Long pharmacistId;
    private String pharmacistName;
    
    // Prescription items
    private List<PrescriptionItemResponseDTO> prescriptionItems;

    public static PrescriptionResponseDTO fromEntity(Prescription prescription) {
        PrescriptionResponseDTO dto = new PrescriptionResponseDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionNumber(prescription.getPrescriptionNumber());
        dto.setDoctorName(prescription.getDoctorName());
        dto.setDoctorLicense(prescription.getDoctorLicense());
        dto.setPrescriptionDate(prescription.getPrescriptionDate());
        dto.setFileUrl(prescription.getFileUrl());
        dto.setFileName(prescription.getFileName());
        dto.setFileType(prescription.getFileType());
        dto.setStatus(prescription.getStatus().toString());
        dto.setNotes(prescription.getNotes());
        dto.setRejectionReason(prescription.getRejectionReason());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setUpdatedAt(prescription.getUpdatedAt());
        dto.setApprovedAt(prescription.getApprovedAt());
        
        // Safely access customer info (should be loaded via JOIN FETCH)
        if (prescription.getCustomer() != null) {
            dto.setCustomerId(prescription.getCustomer().getId());
            String firstName = prescription.getCustomer().getFirstName() != null ? prescription.getCustomer().getFirstName() : "";
            String lastName = prescription.getCustomer().getLastName() != null ? prescription.getCustomer().getLastName() : "";
            dto.setCustomerName((firstName + " " + lastName).trim());
        }
        
        // Safely access pharmacist info (should be loaded via JOIN FETCH)
        if (prescription.getPharmacist() != null) {
            dto.setPharmacistId(prescription.getPharmacist().getId());
            String firstName = prescription.getPharmacist().getFirstName() != null ? prescription.getPharmacist().getFirstName() : "";
            String lastName = prescription.getPharmacist().getLastName() != null ? prescription.getPharmacist().getLastName() : "";
            dto.setPharmacistName((firstName + " " + lastName).trim());
        }
        
        // Transform prescription items to DTOs
        if (prescription.getPrescriptionItems() != null) {
            dto.setPrescriptionItems(
                prescription.getPrescriptionItems().stream()
                    .map(PrescriptionItemResponseDTO::fromEntity)
                    .toList()
            );
        }
        
        return dto;
    }
}
