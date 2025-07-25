package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.repository.MedicineRepository;
import com.philldesk.philldeskbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    @Autowired
    public MedicineServiceImpl(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Medicine> getAllMedicines(Pageable pageable) {
        return medicineRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicine> getMedicineById(Long id) {
        return medicineRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByName(String name) {
        return medicineRepository.searchMedicines(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByManufacturer(String manufacturer) {
        return medicineRepository.findByManufacturer(manufacturer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByCategory(String category) {
        return medicineRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getAvailableMedicines() {
        return medicineRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getLowStockMedicines(Integer threshold) {
        return medicineRepository.findLowStockMedicines();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        // We'll need to implement this query in the repository or filter here
        return medicineRepository.findByIsActiveTrue().stream()
                .filter(m -> m.getUnitPrice().compareTo(minPrice) >= 0 && m.getUnitPrice().compareTo(maxPrice) <= 0)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String searchTerm) {
        return medicineRepository.searchMedicines(searchTerm);
    }

    @Override
    public Medicine saveMedicine(Medicine medicine) {
        medicine.setCreatedAt(LocalDateTime.now());
        medicine.setUpdatedAt(LocalDateTime.now());
        return medicineRepository.save(medicine);
    }

    @Override
    public Medicine updateMedicine(Medicine medicine) {
        Optional<Medicine> existingMedicine = medicineRepository.findById(medicine.getId());
        if (existingMedicine.isPresent()) {
            medicine.setCreatedAt(existingMedicine.get().getCreatedAt());
        }
        medicine.setUpdatedAt(LocalDateTime.now());
        return medicineRepository.save(medicine);
    }

    @Override
    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }

    @Override
    public void updateStock(Long medicineId, Integer newQuantity) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        if (medicine.isPresent()) {
            Medicine existingMedicine = medicine.get();
            existingMedicine.setQuantity(newQuantity);
            existingMedicine.setUpdatedAt(LocalDateTime.now());
            medicineRepository.save(existingMedicine);
        }
    }

    @Override
    public void reduceStock(Long medicineId, Integer quantity) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        if (medicine.isPresent()) {
            Medicine existingMedicine = medicine.get();
            int currentStock = existingMedicine.getQuantity();
            if (currentStock >= quantity) {
                existingMedicine.setQuantity(currentStock - quantity);
                existingMedicine.setUpdatedAt(LocalDateTime.now());
                medicineRepository.save(existingMedicine);
            } else {
                throw new IllegalArgumentException("Insufficient stock. Available: " + currentStock + ", Requested: " + quantity);
            }
        } else {
            throw new IllegalArgumentException("Medicine not found with ID: " + medicineId);
        }
    }

    @Override
    public void increaseStock(Long medicineId, Integer quantity) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        if (medicine.isPresent()) {
            Medicine existingMedicine = medicine.get();
            existingMedicine.setQuantity(existingMedicine.getQuantity() + quantity);
            existingMedicine.setUpdatedAt(LocalDateTime.now());
            medicineRepository.save(existingMedicine);
        } else {
            throw new IllegalArgumentException("Medicine not found with ID: " + medicineId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(Long medicineId, Integer requestedQuantity) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        return medicine.isPresent() && medicine.get().getQuantity() >= requestedQuantity;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        Optional<Medicine> medicine = medicineRepository.findByNameAndIsActiveTrue(name);
        return medicine.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameStrengthAndForm(String name, String strength, String dosageForm) {
        Optional<Medicine> medicine = medicineRepository.findByNameAndStrengthAndDosageForm(name, strength, dosageForm);
        return medicine.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameStrengthFormAndManufacturer(String name, String strength, String dosageForm, String manufacturer) {
        Optional<Medicine> medicine = medicineRepository.findByNameAndStrengthAndDosageFormAndManufacturer(name, strength, dosageForm, manufacturer);
        return medicine.isPresent();
    }
}
