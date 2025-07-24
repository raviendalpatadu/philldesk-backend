package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicines")
@CrossOrigin(origins = "*")
public class MedicineController {

    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        List<Medicine> medicines = medicineService.getAllMedicines();
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Medicine>> getAllMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Medicine> medicines = medicineService.getAllMedicines(pageable);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        Optional<Medicine> medicine = medicineService.getMedicineById(id);
        return medicine.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/{name}")
    public ResponseEntity<List<Medicine>> getMedicinesByName(@PathVariable String name) {
        List<Medicine> medicines = medicineService.getMedicinesByName(name);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/manufacturer/{manufacturer}")
    public ResponseEntity<List<Medicine>> getMedicinesByManufacturer(@PathVariable String manufacturer) {
        List<Medicine> medicines = medicineService.getMedicinesByManufacturer(manufacturer);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Medicine>> getMedicinesByCategory(@PathVariable String category) {
        List<Medicine> medicines = medicineService.getMedicinesByCategory(category);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Medicine>> getAvailableMedicines() {
        List<Medicine> medicines = medicineService.getAvailableMedicines();
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Medicine>> getLowStockMedicines(
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<Medicine> medicines = medicineService.getLowStockMedicines(threshold);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<Medicine>> getMedicinesByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        List<Medicine> medicines = medicineService.getMedicinesByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String searchTerm) {
        List<Medicine> medicines = medicineService.searchMedicines(searchTerm);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<List<Medicine>> getMedicineSuggestions(@RequestParam String query) {
        // Enhanced search for auto-suggestions
        List<Medicine> medicines = medicineService.searchMedicines(query);
        
        // Limit to top 10 results for performance
        List<Medicine> suggestions = medicines.stream()
            .filter(medicine -> medicine.getIsActive() && !medicine.isExpired())
            .limit(10)
            .toList();
            
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(@RequestBody Medicine medicine) {
        try {
            Medicine savedMedicine = medicineService.saveMedicine(medicine);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMedicine);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id, @RequestBody Medicine medicine) {
        Optional<Medicine> existingMedicine = medicineService.getMedicineById(id);
        if (existingMedicine.isPresent()) {
            medicine.setId(id);
            Medicine updatedMedicine = medicineService.updateMedicine(medicine);
            return ResponseEntity.ok(updatedMedicine);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) {
        Optional<Medicine> existingMedicine = medicineService.getMedicineById(id);
        if (existingMedicine.isPresent()) {
            medicineService.deleteMedicine(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            medicineService.updateStock(id, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/reduce-stock")
    public ResponseEntity<Void> reduceStock(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            medicineService.reduceStock(id, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/increase-stock")
    public ResponseEntity<Void> increaseStock(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            medicineService.increaseStock(id, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<Boolean> checkAvailability(@PathVariable Long id, @RequestParam Integer quantity) {
        boolean available = medicineService.isAvailable(id, quantity);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/exists/{name}")
    public ResponseEntity<Boolean> existsByName(@PathVariable String name) {
        boolean exists = medicineService.existsByName(name);
        return ResponseEntity.ok(exists);
    }
}
