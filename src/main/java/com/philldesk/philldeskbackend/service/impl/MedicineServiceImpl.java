package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.repository.MedicineRepository;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(MedicineServiceImpl.class);
    
    private final MedicineRepository medicineRepository;
    private final NotificationService notificationService;

    @Autowired
    public MedicineServiceImpl(MedicineRepository medicineRepository, NotificationService notificationService) {
        this.medicineRepository = medicineRepository;
        this.notificationService = notificationService;
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
            Medicine savedMedicine = medicineRepository.save(existingMedicine);
            
            // Check for low stock and trigger notification if needed
            checkAndNotifyLowStock(savedMedicine);
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
                Medicine savedMedicine = medicineRepository.save(existingMedicine);
                
                // Check for low stock and trigger notification if needed
                checkAndNotifyLowStock(savedMedicine);
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
            Medicine savedMedicine = medicineRepository.save(existingMedicine);
            
            // Note: After increasing stock, we generally won't have low stock, 
            // but including this for consistency and in case reorder levels change
            checkAndNotifyLowStock(savedMedicine);
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

    /**
     * Helper method to check if medicine is low on stock and trigger notification
     * This method is called after every stock update operation
     */
    private void checkAndNotifyLowStock(Medicine medicine) {
        if (medicine.isLowStock()) {
            try {
                notificationService.createLowStockNotification(medicine.getId());
            } catch (Exception e) {
                // Log the error but don't fail the stock operation
                logger.error("Failed to create low stock notification for medicine ID: {}. Error: {}", 
                           medicine.getId(), e.getMessage(), e);
            }
        }
    }
}
