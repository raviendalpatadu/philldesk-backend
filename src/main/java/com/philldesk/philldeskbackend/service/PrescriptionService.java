package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PrescriptionService {
    List<Prescription> getAllPrescriptions();
    Page<Prescription> getAllPrescriptions(Pageable pageable);
    Optional<Prescription> getPrescriptionById(Long id);
    Optional<Prescription> findById(Long id);
    Optional<Prescription> getPrescriptionByIdWithUserDetails(Long id);
    List<Prescription> getPrescriptionsByCustomer(User customer);
    List<Prescription> getPrescriptionsByCustomerId(Long customerId);
    List<Prescription> getPrescriptionsByPharmacist(User pharmacist);
    List<Prescription> getPrescriptionsByPharmacistId(Long pharmacistId);
    List<Prescription> getPrescriptionsByStatus(Prescription.PrescriptionStatus status);
    List<Prescription> getPrescriptionsByDateRange(LocalDate startDate, LocalDate endDate);
    List<Prescription> getPendingPrescriptions();
    List<Prescription> getProcessingPrescriptions();
    List<Prescription> getCompletedPrescriptions();
    Prescription savePrescription(Prescription prescription);
    Prescription updatePrescription(Prescription prescription);
    void deletePrescription(Long id);
    void updateStatus(Long prescriptionId, Prescription.PrescriptionStatus status);
    void assignPharmacist(Long prescriptionId, Long pharmacistId);
    boolean canBeFulfilled(Long prescriptionId);
    List<Prescription> searchPrescriptions(String searchTerm);
    
    // Enhanced completion methods
    void updateCompletionDetails(Long prescriptionId, Map<String, Object> completionData);
    void createDispensingRecord(Long prescriptionId, Map<String, Object> dispensingRecord);
    Map<String, Object> getCompletionDetails(Long prescriptionId);
}
