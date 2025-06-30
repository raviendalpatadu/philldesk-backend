package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.repository.PrescriptionItemRepository;
import com.philldesk.philldeskbackend.service.PrescriptionItemService;
import com.philldesk.philldeskbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PrescriptionItemServiceImpl implements PrescriptionItemService {

    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineService medicineService;

    @Autowired
    public PrescriptionItemServiceImpl(PrescriptionItemRepository prescriptionItemRepository,
                                     MedicineService medicineService) {
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicineService = medicineService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> getAllPrescriptionItems() {
        return prescriptionItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrescriptionItem> getPrescriptionItemById(Long id) {
        return prescriptionItemRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> getPrescriptionItemsByPrescription(Prescription prescription) {
        return prescriptionItemRepository.findByPrescription(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> getPrescriptionItemsByPrescriptionId(Long prescriptionId) {
        return prescriptionItemRepository.findByPrescriptionId(prescriptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> getPrescriptionItemsByMedicine(Medicine medicine) {
        return prescriptionItemRepository.findByMedicine(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> getPrescriptionItemsByMedicineId(Long medicineId) {
        return prescriptionItemRepository.findByMedicineId(medicineId);
    }

    @Override
    public PrescriptionItem savePrescriptionItem(PrescriptionItem prescriptionItem) {
        return prescriptionItemRepository.save(prescriptionItem);
    }

    @Override
    public PrescriptionItem updatePrescriptionItem(PrescriptionItem prescriptionItem) {
        return prescriptionItemRepository.save(prescriptionItem);
    }

    @Override
    public void deletePrescriptionItem(Long id) {
        prescriptionItemRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateAvailability(PrescriptionItem prescriptionItem) {
        return medicineService.isAvailable(prescriptionItem.getMedicine().getId(), 
                                          prescriptionItem.getQuantity());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionItem> validatePrescriptionAvailability(Long prescriptionId) {
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(prescriptionId);
        List<PrescriptionItem> unavailableItems = new ArrayList<>();
        
        for (PrescriptionItem item : items) {
            if (!medicineService.isAvailable(item.getMedicine().getId(), item.getQuantity())) {
                unavailableItems.add(item);
            }
        }
        
        return unavailableItems;
    }
}
