package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.repository.PrescriptionRepository;
import com.philldesk.philldeskbackend.repository.UserRepository;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final MedicineService medicineService;

    @Autowired
    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository, 
                                 UserRepository userRepository,
                                 MedicineService medicineService) {
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.medicineService = medicineService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Prescription> getAllPrescriptions(Pageable pageable) {
        return prescriptionRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByCustomer(User customer) {
        return prescriptionRepository.findByCustomer(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByCustomerId(Long customerId) {
        return prescriptionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByPharmacist(User pharmacist) {
        return prescriptionRepository.findByPharmacist(pharmacist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByPharmacistId(Long pharmacistId) {
        return prescriptionRepository.findByPharmacistIdOrderByCreatedAtDesc(pharmacistId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByStatus(Prescription.PrescriptionStatus status) {
        return prescriptionRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return prescriptionRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPendingPrescriptions() {
        return prescriptionRepository.findByStatus(Prescription.PrescriptionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getProcessingPrescriptions() {
        return prescriptionRepository.findByStatus(Prescription.PrescriptionStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getCompletedPrescriptions() {
        return prescriptionRepository.findByStatus(Prescription.PrescriptionStatus.COMPLETED);
    }

    @Override
    public Prescription savePrescription(Prescription prescription) {
        prescription.setCreatedAt(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        if (prescription.getStatus() == null) {
            prescription.setStatus(Prescription.PrescriptionStatus.PENDING);
        }
        return prescriptionRepository.save(prescription);
    }

    @Override
    public Prescription updatePrescription(Prescription prescription) {
        Optional<Prescription> existingPrescription = prescriptionRepository.findById(prescription.getId());
        if (existingPrescription.isPresent()) {
            prescription.setCreatedAt(existingPrescription.get().getCreatedAt());
        }
        prescription.setUpdatedAt(LocalDateTime.now());
        return prescriptionRepository.save(prescription);
    }

    @Override
    public void deletePrescription(Long id) {
        prescriptionRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long prescriptionId, Prescription.PrescriptionStatus status) {
        Optional<Prescription> prescription = prescriptionRepository.findById(prescriptionId);
        if (prescription.isPresent()) {
            Prescription existingPrescription = prescription.get();
            existingPrescription.setStatus(status);
            existingPrescription.setUpdatedAt(LocalDateTime.now());
            prescriptionRepository.save(existingPrescription);
        }
    }

    @Override
    public void assignPharmacist(Long prescriptionId, Long pharmacistId) {
        Optional<Prescription> prescription = prescriptionRepository.findById(prescriptionId);
        Optional<User> pharmacist = userRepository.findById(pharmacistId);
        
        if (prescription.isPresent() && pharmacist.isPresent()) {
            Prescription existingPrescription = prescription.get();
            existingPrescription.setPharmacist(pharmacist.get());
            existingPrescription.setUpdatedAt(LocalDateTime.now());
            prescriptionRepository.save(existingPrescription);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canBeFulfilled(Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionRepository.findById(prescriptionId);
        if (prescription.isPresent()) {
            Prescription p = prescription.get();
            // Check if all prescription items can be fulfilled
            for (PrescriptionItem item : p.getPrescriptionItems()) {
                if (!medicineService.isAvailable(item.getMedicine().getId(), item.getQuantity())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> searchPrescriptions(String searchTerm) {
        return prescriptionRepository.searchPrescriptions(searchTerm);
    }
}
