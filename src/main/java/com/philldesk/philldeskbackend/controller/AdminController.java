package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Controller
 * 
 * This controller provides admin-specific endpoints for inventory management,
 * user management, and system administration functions.
 * All endpoints require ADMIN role authorization.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";

    private final MedicineService medicineService;
    private final UserService userService;

    @Autowired
    public AdminController(MedicineService medicineService, UserService userService) {
        this.medicineService = medicineService;
        this.userService = userService;
    }

    // ========================================
    // Medicine/Inventory Management
    // ========================================

    /**
     * Get all medicines with pagination and sorting
     */
    @GetMapping("/medicines")
    public ResponseEntity<Page<Medicine>> getAllMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : 
                       Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Medicine> medicines = medicineService.getAllMedicines(pageable);
            return ResponseEntity.ok(medicines);
        } catch (Exception e) {
            logger.error("Error fetching medicines: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all medicines (non-paginated)
     */
    @GetMapping("/medicines/all")
    public ResponseEntity<List<Medicine>> getAllMedicinesNonPaginated() {
        try {
            List<Medicine> medicines = medicineService.getAllMedicines();
            return ResponseEntity.ok(medicines);
        } catch (Exception e) {
            logger.error("Error fetching all medicines: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new medicine
     */
    @PostMapping("/medicines")
    public ResponseEntity<Map<String, Object>> createMedicine(@RequestBody Medicine medicine) {
        try {
            // Check if medicine with the same name, strength, dosage form, and manufacturer already exists
            if (medicineService.existsByNameStrengthFormAndManufacturer(
                    medicine.getName(), 
                    medicine.getStrength(), 
                    medicine.getDosageForm(),
                    medicine.getManufacturer())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Medicine '" + medicine.getName() + 
                               " " + medicine.getStrength() + " " + medicine.getDosageForm() + 
                               " by " + medicine.getManufacturer() + "' already exists");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Medicine savedMedicine = medicineService.saveMedicine(medicine);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Medicine created successfully");
            response.put("medicine", savedMedicine);
            
            logger.info("Medicine created successfully: {}", savedMedicine.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating medicine: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to create medicine: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update existing medicine
     */
    @PutMapping("/medicines/{id}")
    public ResponseEntity<Map<String, Object>> updateMedicine(
            @PathVariable Long id, 
            @RequestBody Medicine medicine) {
        try {
            Optional<Medicine> existingMedicine = medicineService.getMedicineById(id);
            if (existingMedicine.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Medicine not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            medicine.setId(id);
            Medicine updatedMedicine = medicineService.updateMedicine(medicine);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Medicine updated successfully");
            response.put("medicine", updatedMedicine);
            
            logger.info("Medicine updated successfully: {}", updatedMedicine.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating medicine: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to update medicine: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete medicine
     */
    @DeleteMapping("/medicines/{id}")
    public ResponseEntity<Map<String, String>> deleteMedicine(@PathVariable Long id) {
        try {
            Optional<Medicine> existingMedicine = medicineService.getMedicineById(id);
            if (existingMedicine.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Medicine not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            String medicineName = existingMedicine.get().getName();
            medicineService.deleteMedicine(id);
            
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Medicine '" + medicineName + "' deleted successfully");
            
            logger.info("Medicine deleted successfully: {}", medicineName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting medicine: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to delete medicine: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update medicine stock
     */
    @PatchMapping("/medicines/{id}/stock")
    public ResponseEntity<Map<String, String>> updateMedicineStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "set") String operation) {
        try {
            switch (operation.toLowerCase()) {
                case "increase":
                    medicineService.increaseStock(id, quantity);
                    break;
                case "decrease":
                    medicineService.reduceStock(id, quantity);
                    break;
                case "set":
                default:
                    medicineService.updateStock(id, quantity);
                    break;
            }
            
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Stock updated successfully");
            
            logger.info("Stock updated for medicine ID {}: {} {}", id, operation, quantity);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error updating stock: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to update stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get inventory statistics
     */
    @GetMapping("/inventory/stats")
    public ResponseEntity<Map<String, Object>> getInventoryStats() {
        try {
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            
            long totalItems = allMedicines.size();
            long inStock = allMedicines.stream()
                .filter(m -> m.getQuantity() > m.getReorderLevel())
                .count();
            long lowStock = allMedicines.stream()
                .filter(m -> m.getQuantity() <= m.getReorderLevel() && m.getQuantity() > 0)
                .count();
            long outOfStock = allMedicines.stream()
                .filter(m -> m.getQuantity() == 0)
                .count();
            
            BigDecimal totalValue = allMedicines.stream()
                .map(m -> m.getUnitPrice().multiply(BigDecimal.valueOf(m.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long suppliersCount = allMedicines.stream()
                .map(Medicine::getManufacturer)
                .filter(manufacturer -> manufacturer != null && !manufacturer.trim().isEmpty())
                .distinct()
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalItems", totalItems);
            stats.put("inStock", inStock);
            stats.put("lowStock", lowStock);
            stats.put("outOfStock", outOfStock);
            stats.put("totalValue", totalValue);
            stats.put("suppliersCount", suppliersCount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error calculating inventory stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get medicines by category
     */
    @GetMapping("/medicines/category/{category}")
    public ResponseEntity<List<Medicine>> getMedicinesByCategory(@PathVariable String category) {
        try {
            List<Medicine> medicines = medicineService.getMedicinesByCategory(category);
            return ResponseEntity.ok(medicines);
        } catch (Exception e) {
            logger.error("Error fetching medicines by category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get low stock medicines
     */
    @GetMapping("/medicines/low-stock")
    public ResponseEntity<List<Medicine>> getLowStockMedicines(
            @RequestParam(defaultValue = "10") Integer threshold) {
        try {
            List<Medicine> medicines = medicineService.getLowStockMedicines(threshold);
            return ResponseEntity.ok(medicines);
        } catch (Exception e) {
            logger.error("Error fetching low stock medicines: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search medicines
     */
    @GetMapping("/medicines/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String searchTerm) {
        try {
            List<Medicine> medicines = medicineService.searchMedicines(searchTerm);
            return ResponseEntity.ok(medicines);
        } catch (Exception e) {
            logger.error("Error searching medicines: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================
    // System Management
    // ========================================

    /**
     * Get system overview
     */
    @GetMapping("/system/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // Medicine statistics
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            overview.put("totalMedicines", allMedicines.size());
            overview.put("activeMedicines", allMedicines.stream()
                .filter(Medicine::getIsActive)
                .count());
            
            // User statistics
            List<User> allUsers = userService.getAllUsers();
            overview.put("totalUsers", allUsers.size());
            overview.put("activeUsers", allUsers.stream()
                .filter(User::getIsActive)
                .count());
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Error fetching system overview: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
