package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;

import java.util.List;
import java.util.Optional;

public interface PrescriptionItemService {
    List<PrescriptionItem> getAllPrescriptionItems();
    Optional<PrescriptionItem> getPrescriptionItemById(Long id);
    List<PrescriptionItem> getPrescriptionItemsByPrescription(Prescription prescription);
    List<PrescriptionItem> getPrescriptionItemsByPrescriptionId(Long prescriptionId);
    List<PrescriptionItem> getPrescriptionItemsByMedicine(Medicine medicine);
    List<PrescriptionItem> getPrescriptionItemsByMedicineId(Long medicineId);
    PrescriptionItem savePrescriptionItem(PrescriptionItem prescriptionItem);
    PrescriptionItem updatePrescriptionItem(PrescriptionItem prescriptionItem);
    void deletePrescriptionItem(Long id);
    boolean validateAvailability(PrescriptionItem prescriptionItem);
    List<PrescriptionItem> validatePrescriptionAvailability(Long prescriptionId);
}
