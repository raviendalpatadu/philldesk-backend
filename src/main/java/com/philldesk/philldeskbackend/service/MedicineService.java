package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MedicineService {
    List<Medicine> getAllMedicines();
    Page<Medicine> getAllMedicines(Pageable pageable);
    Optional<Medicine> getMedicineById(Long id);
    List<Medicine> getMedicinesByName(String name);
    List<Medicine> getMedicinesByManufacturer(String manufacturer);
    List<Medicine> getMedicinesByCategory(String category);
    List<Medicine> getAvailableMedicines();
    List<Medicine> getLowStockMedicines(Integer threshold);
    List<Medicine> getMedicinesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    List<Medicine> searchMedicines(String searchTerm);
    Medicine saveMedicine(Medicine medicine);
    Medicine updateMedicine(Medicine medicine);
    void deleteMedicine(Long id);
    void updateStock(Long medicineId, Integer newQuantity);
    void reduceStock(Long medicineId, Integer quantity);
    void increaseStock(Long medicineId, Integer quantity);
    boolean isAvailable(Long medicineId, Integer requestedQuantity);
    boolean existsByName(String name);
    boolean existsByNameStrengthAndForm(String name, String strength, String dosageForm);
    boolean existsByNameStrengthFormAndManufacturer(String name, String strength, String dosageForm, String manufacturer);
}
