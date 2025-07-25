package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.dto.ReadyForPickupPrescriptionDTO;
import com.philldesk.philldeskbackend.entity.*;
import com.philldesk.philldeskbackend.security.UserPrincipal;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.service.UserService;
import com.philldesk.philldeskbackend.service.BillService;
import com.philldesk.philldeskbackend.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/pharmacist")
@CrossOrigin(origins = "*")
public class PharmacistController {

    private static final String MESSAGE_KEY = "message";
    private static final Logger logger = LoggerFactory.getLogger(PharmacistController.class);

    private final PrescriptionService prescriptionService;
    private final MedicineService medicineService;
    private final UserService userService;
    private final BillService billService;
    private final RoleService roleService;

    @Autowired
    public PharmacistController(PrescriptionService prescriptionService, 
                               MedicineService medicineService,
                               UserService userService,
                               BillService billService,
                               RoleService roleService) {
        this.prescriptionService = prescriptionService;
        this.medicineService = medicineService;
        this.userService = userService;
        this.billService = billService;
        this.roleService = roleService;
    }

    /**
     * Get pharmacist dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Prescription statistics
            List<Prescription> allPrescriptions = prescriptionService.getAllPrescriptions();
            stats.put("total", allPrescriptions.size());
            stats.put("pending", prescriptionService.getPendingPrescriptions().size());
            stats.put("underReview", allPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.APPROVED)
                .count());
            stats.put("readyForPickup", allPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.READY_FOR_PICKUP)
                .count());
            stats.put("completed", prescriptionService.getCompletedPrescriptions().size());
            
            // Today's statistics
            LocalDate today = LocalDate.now();
            
            List<Prescription> todayPrescriptions = prescriptionService.getPrescriptionsByDateRange(today, today);
            stats.put("approvedToday", todayPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.APPROVED ||
                           p.getStatus() == Prescription.PrescriptionStatus.DISPENSED ||
                           p.getStatus() == Prescription.PrescriptionStatus.COMPLETED)
                .count());
            
            // Emergency prescriptions
            stats.put("emergency", allPrescriptions.stream()
                .filter(p -> p.getNotes() != null && 
                           (p.getNotes().toLowerCase().contains("emergency") ||
                            p.getNotes().toLowerCase().contains("urgent")))
                .count());
            
            // Inventory statistics
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            stats.put("totalInventoryItems", allMedicines.size());
            stats.put("lowStockItems", medicineService.getLowStockMedicines(50).size());
            stats.put("outOfStockItems", allMedicines.stream()
                .filter(m -> m.getQuantity() <= 0)
                .count());
            stats.put("criticalLowItems", medicineService.getLowStockMedicines(10).size());
            
            double totalValue = allMedicines.stream()
                .mapToDouble(m -> m.getUnitPrice().doubleValue() * m.getQuantity())
                .sum();
            stats.put("totalInventoryValue", totalValue);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pending prescriptions for review
     */
    @GetMapping("/prescriptions/pending")
    public ResponseEntity<List<Prescription>> getPendingPrescriptions() {
        try {
            List<Prescription> prescriptions = prescriptionService.getPendingPrescriptions();
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all prescriptions with pagination support
     */
    @GetMapping("/prescriptions")
    public ResponseEntity<List<Prescription>> getAllPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
            // For now, return all prescriptions. Later can add pagination with Page<Prescription>
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get prescriptions that require clarification
     */
    @GetMapping("/prescriptions/requires-clarification")
    public ResponseEntity<List<Prescription>> getRequiresClarificationPrescriptions() {
        try {
            List<Prescription> allPrescriptions = prescriptionService.getAllPrescriptions();
            List<Prescription> requiresClarification = allPrescriptions.stream()
                .filter(p -> p.getRejectionReason() != null && 
                           p.getStatus() == Prescription.PrescriptionStatus.REJECTED)
                .toList();
            return ResponseEntity.ok(requiresClarification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Review and approve/reject prescription
     */
    @PostMapping("/prescriptions/{id}/review")
    public ResponseEntity<Map<String, String>> reviewPrescription(
            @PathVariable Long id,
            @RequestBody Map<String, Object> reviewData) {
        try {
            String decision = (String) reviewData.get("decision");
            String notes = (String) reviewData.get("notes");
            
            Optional<Prescription> prescriptionOpt = prescriptionService.getPrescriptionById(id);
            if (prescriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Prescription prescription = prescriptionOpt.get();
            Prescription.PrescriptionStatus newStatus;

            // check for prescrition items
            if (prescription.getPrescriptionItems().isEmpty()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Prescrition Items must be selected and saved "));
            }
            
            switch (decision.toLowerCase()) {
                case "approve":
                case "approve_hold":

                    newStatus = Prescription.PrescriptionStatus.APPROVED;
                    prescription.setApprovedAt(LocalDateTime.now());
                    if (notes != null) {
                        prescription.setNotes(notes);
                    }
                    
                    // Update status and save prescription first
                    prescription.setStatus(newStatus);
                    prescriptionService.updatePrescription(prescription);
                    
                    // Automatically create bill for approved prescription
                    try {
                        // Check if prescription has items before generating bill
                        if (prescription.getPrescriptionItems() == null || prescription.getPrescriptionItems().isEmpty()) {
                            Map<String, String> response = new HashMap<>();
                            response.put(MESSAGE_KEY, "Prescription approved successfully. Please add medicines first, then regenerate the bill.");
                            response.put("status", newStatus.toString());
                            response.put("decision", decision);
                            response.put("warning", "No prescription items found - add medicines to generate bill");
                            return ResponseEntity.ok(response);
                        }
                        
                        // Check if bill already exists for this prescription
                        Optional<Bill> existingBill = billService.getBillByPrescription(prescription);
                        if (existingBill.isEmpty()) {
                            // Generate new bill
                            Bill newBill = billService.generateBillFromPrescription(prescription);
                            
                            // Log bill generation for customer notification
                            logger.info("Bill generated for prescription {}: Bill ID {}, Amount {}", 
                                prescription.getPrescriptionNumber(), newBill.getId(), newBill.getTotalAmount());
                            
                            Map<String, String> response = new HashMap<>();
                            response.put(MESSAGE_KEY, "Prescription approved and bill generated successfully! Customer will be notified.");
                            response.put("status", newStatus.toString());
                            response.put("decision", decision);
                            response.put("billId", newBill.getId().toString());
                            response.put("billNumber", newBill.getBillNumber());
                            response.put("billItemsCount", String.valueOf(prescription.getPrescriptionItems().size()));
                            response.put("totalAmount", newBill.getTotalAmount().toString());
                            response.put("customerNotified", "true");
                            return ResponseEntity.ok(response);
                        } else {
                            Map<String, String> response = new HashMap<>();
                            response.put(MESSAGE_KEY, "Prescription approved successfully (bill already exists)");
                            response.put("status", newStatus.toString());
                            response.put("decision", decision);
                            response.put("existingBillId", existingBill.get().getId().toString());
                            response.put("existingBillNumber", existingBill.get().getBillNumber());
                            return ResponseEntity.ok(response);
                        }
                    } catch (Exception billException) {
                        // If bill creation fails, still return success for prescription approval
                        logger.error("Bill creation failed for prescription {}: {}", prescription.getPrescriptionNumber(), billException.getMessage());
                        Map<String, String> response = new HashMap<>();
                        response.put(MESSAGE_KEY, "Prescription approved successfully, but bill creation failed. Please generate bill manually.");
                        response.put("status", newStatus.toString());
                        response.put("decision", decision);
                        response.put("warning", "Bill creation failed - please create manually from billing management");
                        response.put("error", billException.getMessage());
                        return ResponseEntity.ok(response);
                    }
                    
                case "clarification":
                case "reject":
                    newStatus = Prescription.PrescriptionStatus.REJECTED;
                    if (notes != null) {
                        prescription.setRejectionReason(notes);
                    }
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid decision: " + decision));
            }
            
            // Update status and save prescription for rejection
            prescription.setStatus(newStatus);
            prescriptionService.updatePrescription(prescription);
            
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription reviewed successfully");
            response.put("status", newStatus.toString());
            response.put("decision", decision);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to review prescription: " + e.getMessage()));
        }
    }

    /**
     * Get prescriptions assigned to a specific pharmacist
     */
    @GetMapping("/prescriptions/assigned/{pharmacistId}")
    public ResponseEntity<List<Prescription>> getAssignedPrescriptions(@PathVariable Long pharmacistId) {
        try {
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPharmacistId(pharmacistId);
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark prescription as ready for pickup
     */
    @PostMapping("/prescriptions/{id}/ready")
    public ResponseEntity<Map<String, String>> markReadyForPickup(@PathVariable Long id) {
        try {
            // Enhanced payment validation logic
            billService.getBillByPrescription(prescriptionService.getPrescriptionById(id).orElseThrow())
                .ifPresent(bill -> {
                    if (bill.getPaymentType() == Bill.PaymentType.ONLINE && bill.getPaymentStatus() != Bill.PaymentStatus.PAID) {
                        throw new IllegalStateException("Cannot mark prescription as ready for pickup - online payment not completed");
                    }
                    // For PAY_ON_PICKUP, we allow the prescription to be marked ready even with PENDING payment
                    // Payment will be collected when customer arrives
                });

            // stock should get reduced before marking as ready
            prescriptionService.getPrescriptionById(id).ifPresent(prescription -> {
                prescription.getPrescriptionItems().forEach(item -> {
                    Medicine medicine = medicineService.getMedicineById(item.getMedicine().getId())
                        .orElseThrow(() -> new IllegalStateException("Medicine not found: " + item.getMedicine().getId()));
                    if (medicine.getQuantity() < item.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock for medicine: " + medicine.getName());
                    }
                    medicineService.reduceStock(medicine.getId(), item.getQuantity());
                });
            });

            // Update prescription status to READY_FOR_PICKUP instead of DISPENSED
            prescriptionService.updateStatus(id, Prescription.PrescriptionStatus.READY_FOR_PICKUP);
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription marked as ready for pickup");
            response.put("status", "READY_FOR_PICKUP");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error marking prescription {} as ready for pickup: {}", id, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to mark prescription as ready: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Collect payment for a prescription when customer arrives for pickup
     */
    @PostMapping("/prescriptions/{id}/collect-payment")
    public ResponseEntity<Map<String, Object>> collectPayment(
            @PathVariable Long id, 
            @RequestBody Map<String, String> paymentData) {
        try {
            String paymentMethod = paymentData.get("paymentMethod");
            String notes = paymentData.get("notes");
            
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Payment method is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Get the prescription and associated bill
            Optional<Prescription> prescriptionOpt = prescriptionService.getPrescriptionById(id);
            if (prescriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Prescription prescription = prescriptionOpt.get();
            
            // Check if prescription is ready for pickup
            if (prescription.getStatus() != Prescription.PrescriptionStatus.READY_FOR_PICKUP) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prescription is not ready for pickup");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Get the associated bill
            Optional<Bill> billOpt = billService.getBillByPrescription(prescription);
            if (billOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No bill found for this prescription");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Bill bill = billOpt.get();
            
            // Check if bill is set for pay on pickup and still pending
            if (bill.getPaymentType() != Bill.PaymentType.PAY_ON_PICKUP) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "This bill is not set for pay on pickup");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (bill.getPaymentStatus() != Bill.PaymentStatus.PENDING) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Payment has already been processed");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Process the payment and add notes in one operation
            Bill.PaymentMethod method = Bill.PaymentMethod.valueOf(paymentMethod.toUpperCase());
            
            // Add notes to the bill before marking as paid
            if (notes != null && !notes.trim().isEmpty()) {
                bill.setNotes(bill.getNotes() != null ? bill.getNotes() + "; Pickup: " + notes : "Pickup: " + notes);
                billService.updateBill(bill);
            }
            
            // Mark as paid (this will fetch and update the latest bill)
            billService.markAsPaid(bill.getId(), method);
            
            // Update prescription status to DISPENSED
            prescriptionService.updateStatus(id, Prescription.PrescriptionStatus.COMPLETED);

            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Payment collected successfully and prescription dispensed");
            response.put("billId", bill.getId());
            response.put("amount", bill.getTotalAmount());
            response.put("paymentMethod", paymentMethod);
            response.put("prescriptionStatus", "DISPENSED");
            response.put("paymentStatus", "PAID");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment method for prescription {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid payment method: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error collecting payment for prescription {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to collect payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get prescriptions ready for pickup (awaiting payment collection)
     */
    @GetMapping("/prescriptions/ready-for-pickup")
    public ResponseEntity<List<ReadyForPickupPrescriptionDTO>> getPrescriptionsReadyForPickup() {
        try {
            List<Prescription> readyPrescriptions = prescriptionService.getPrescriptionsByStatus(
                Prescription.PrescriptionStatus.READY_FOR_PICKUP
            );
            
            // Convert to DTOs to avoid nested serialization issues
            List<ReadyForPickupPrescriptionDTO> dtos = readyPrescriptions.stream()
                .map(ReadyForPickupPrescriptionDTO::fromPrescription)
                .toList();
                
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching prescriptions ready for pickup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete prescription (mark as picked up)
     */
    @PostMapping("/prescriptions/{id}/complete")
    public ResponseEntity<Map<String, String>> completePrescription(@PathVariable Long id) {
        try {
            prescriptionService.updateStatus(id, Prescription.PrescriptionStatus.COMPLETED);
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get inventory management data
     */
    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryData() {
        try {
            Map<String, Object> inventoryData = new HashMap<>();
            
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            inventoryData.put("medicines", allMedicines);
            
            List<Medicine> lowStock = medicineService.getLowStockMedicines(50);
            inventoryData.put("lowStock", lowStock);
            
            List<Medicine> criticalLow = medicineService.getLowStockMedicines(10);
            inventoryData.put("criticalLow", criticalLow);
            
            List<Medicine> outOfStock = allMedicines.stream()
                .filter(m -> m.getQuantity() <= 0)
                .toList();
            inventoryData.put("outOfStock", outOfStock);
            
            return ResponseEntity.ok(inventoryData);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve inventory data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", errorResponse));
        }
    }

    /**
     * Update medicine stock
     */
    @PatchMapping("/inventory/{medicineId}/stock")
    public ResponseEntity<Map<String, String>> updateMedicineStock(
            @PathVariable Long medicineId,
            @RequestParam Integer quantity,
            @RequestParam String operation) {
        try {
            switch (operation.toLowerCase()) {
                case "increase":
                    medicineService.increaseStock(medicineId, quantity);
                    break;
                case "decrease":
                    medicineService.reduceStock(medicineId, quantity);
                    break;
                case "set":
                    medicineService.updateStock(medicineId, quantity);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid operation: " + operation));
            }
            
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Stock updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search prescriptions by patient name, prescription number, or doctor name
     */
    @GetMapping("/prescriptions/search")
    public ResponseEntity<List<Prescription>> searchPrescriptions(@RequestParam String query) {
        try {
            List<Prescription> prescriptions = prescriptionService.searchPrescriptions(query);
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get prescription workflow statistics
     */
    @GetMapping("/workflow/stats")
    public ResponseEntity<Map<String, Object>> getWorkflowStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Today's workflow
            LocalDate today = LocalDate.now();
            List<Prescription> todayPrescriptions = prescriptionService.getPrescriptionsByDateRange(today, today);
            
            stats.put("submittedToday", todayPrescriptions.size());
            stats.put("reviewedToday", todayPrescriptions.stream()
                .filter(p -> p.getStatus() != Prescription.PrescriptionStatus.PENDING)
                .count());
            stats.put("completedToday", todayPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.COMPLETED)
                .count());
            
            // Average processing time (simplified - could be enhanced with actual timing data)
            stats.put("averageProcessingTimeHours", 2.5);
            stats.put("averagePickupTimeHours", 4.0);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all pharmacists for assignment
     */
    @GetMapping("/pharmacists")
    public ResponseEntity<List<User>> getAllPharmacists() {
        try {
            List<User> pharmacists = userService.getUsersByRole(Role.RoleName.PHARMACIST);
            return ResponseEntity.ok(pharmacists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign prescription to pharmacist
     */
    @PostMapping("/prescriptions/{id}/assign")
    public ResponseEntity<Map<String, String>> assignPrescription(
            @PathVariable Long id,
            @RequestParam Long pharmacistId) {
        try {
            prescriptionService.assignPharmacist(id, pharmacistId);
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription assigned successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete prescription with enhanced details
     */
    @PostMapping("/prescriptions/{id}/complete-with-details")
    public ResponseEntity<Map<String, Object>> completeWithDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> completionDetails) {
        try {
            // Update prescription status to completed
            prescriptionService.updateStatus(id, Prescription.PrescriptionStatus.COMPLETED);
            
            // Extract completion details
            String instructions = (String) completionDetails.get("instructions");
            String followUpDate = (String) completionDetails.get("followUpDate");
            String dispensingNotes = (String) completionDetails.get("dispensingNotes");
            List<Map<String, Object>> medicineDetails = (List<Map<String, Object>>) completionDetails.get("medicineDetails");
            
            // Store completion metadata
            Map<String, Object> completionData = new HashMap<>();
            completionData.put("completedAt", LocalDateTime.now());
            completionData.put("instructions", instructions);
            completionData.put("followUpDate", followUpDate);
            completionData.put("dispensingNotes", dispensingNotes);
            completionData.put("medicineDetails", medicineDetails);
            
            // Update prescription with completion details
            prescriptionService.updateCompletionDetails(id, completionData);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription completed successfully with details");
            response.put("completionId", id);
            response.put("completedAt", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to complete prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate receipt for completed prescription
     */
    @GetMapping("/prescriptions/{id}/receipt")
    public ResponseEntity<Map<String, Object>> generateReceipt(@PathVariable Long id) {
        try {
            Optional<Prescription> prescriptionOpt = prescriptionService.findById(id);
            if (prescriptionOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prescription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            Prescription prescription = prescriptionOpt.get();
            if (prescription.getStatus() != Prescription.PrescriptionStatus.COMPLETED) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prescription is not completed yet");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Generate receipt data
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("prescriptionId", prescription.getId());
            receipt.put("customerName", prescription.getCustomer().getFirstName() + " " + prescription.getCustomer().getLastName());
            receipt.put("customerEmail", prescription.getCustomer().getEmail());
            receipt.put("doctorName", prescription.getDoctorName());
            receipt.put("completedDate", prescription.getUpdatedAt());
            receipt.put("pharmacistName", prescription.getPharmacist() != null ? 
                prescription.getPharmacist().getFirstName() + " " + prescription.getPharmacist().getLastName() : "Unknown");
            receipt.put("status", prescription.getStatus().toString());
            receipt.put("instructions", prescription.getNotes());
            
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create dispensing record for regulatory compliance
     */
    @PostMapping("/prescriptions/{id}/dispense-record")
    public ResponseEntity<Map<String, Object>> createDispensingRecord(
            @PathVariable Long id,
            @RequestBody Map<String, Object> dispensingDetails) {
        try {
            // Extract dispensing details
            List<Map<String, Object>> medicineRecords = (List<Map<String, Object>>) dispensingDetails.get("medicineRecords");
            String pharmacistSignature = (String) dispensingDetails.get("pharmacistSignature");
            String dispensingDate = (String) dispensingDetails.get("dispensingDate");
            
            // Create dispensing record
            Map<String, Object> dispensingRecord = new HashMap<>();
            dispensingRecord.put("prescriptionId", id);
            dispensingRecord.put("medicineRecords", medicineRecords);
            dispensingRecord.put("pharmacistSignature", pharmacistSignature);
            dispensingRecord.put("dispensingDate", dispensingDate);
            dispensingRecord.put("createdAt", LocalDateTime.now());
            
            // Store dispensing record (in a real system, this would go to a separate table)
            prescriptionService.createDispensingRecord(id, dispensingRecord);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Dispensing record created successfully");
            response.put("recordId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create dispensing record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get completion details for a prescription
     */
    @GetMapping("/prescriptions/{id}/completion-details")
    public ResponseEntity<Map<String, Object>> getCompletionDetails(@PathVariable Long id) {
        try {
            Map<String, Object> completionDetails = prescriptionService.getCompletionDetails(id);
            if (completionDetails.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Completion details not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(completionDetails);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get completion details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ===========================
    // MANUAL BILLING ENDPOINTS
    // ===========================

    /**
     * Get all medicines for manual billing
     */
    @GetMapping("/manual-billing/medicines")
    public ResponseEntity<Map<String, Object>> getAllMedicinesForBilling() {
        try {
            List<Medicine> medicines = medicineService.getAllMedicines();
            
            // Filter out medicines with zero stock for manual billing
            List<Medicine> availableMedicines = medicines.stream()
                .filter(medicine -> medicine.getQuantity() > 0)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("medicines", availableMedicines);
            response.put("total", availableMedicines.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching medicines for manual billing: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch medicines: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search medicines for manual billing
     */
    @GetMapping("/manual-billing/medicines/search")
    public ResponseEntity<List<Medicine>> searchMedicinesForBilling(@RequestParam String query) {
        try {
            if (query == null || query.trim().length() < 2) {
                return ResponseEntity.ok(List.of());
            }
            
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            
            // Search by name, category, or manufacturer (case-insensitive)
            String searchQuery = query.toLowerCase().trim();
            List<Medicine> searchResults = allMedicines.stream()
                .filter(medicine -> medicine.getQuantity() > 0) // Only available medicines
                .filter(medicine -> 
                    medicine.getName().toLowerCase().contains(searchQuery) ||
                    (medicine.getCategory() != null && medicine.getCategory().toLowerCase().contains(searchQuery)) ||
                    (medicine.getManufacturer() != null && medicine.getManufacturer().toLowerCase().contains(searchQuery))
                )
                .limit(20) // Limit results for performance
                .toList();
            
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            logger.error("Error searching medicines for manual billing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Generate manual bill for walk-in customers
     */
    @PostMapping("/manual-billing/generate-bill")
    public ResponseEntity<Map<String, Object>> generateManualBill(@RequestBody Map<String, Object> billData) {
        try {
            // Extract bill data
            @SuppressWarnings("unchecked")
            Map<String, String> customerData = (Map<String, String>) billData.get("customer");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) billData.get("items");
            Double subtotal = ((Number) billData.get("subtotal")).doubleValue();
            Double discount = ((Number) billData.get("discount")).doubleValue();
            Double total = ((Number) billData.get("total")).doubleValue();
            String paymentMethod = (String) billData.get("paymentMethod");
            Double receivedAmount = billData.get("receivedAmount") != null ?
                    ((Number) billData.get("receivedAmount")).doubleValue() : total;
            Double changeAmount = billData.get("changeAmount") != null ?
                    ((Number) billData.get("changeAmount")).doubleValue() : 0.0;

            // Validate required fields
            if (customerData == null || customerData.get("name") == null || customerData.get("name").trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Customer name is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (items == null || items.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "At least one item is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate stock availability for all items
            for (Map<String, Object> item : items) {
                Long medicineId = ((Number) item.get("medicineId")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();

                Optional<Medicine> medicineOpt = medicineService.getMedicineById(medicineId);
                if (medicineOpt.isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Medicine not found: " + medicineId);
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                Medicine medicine = medicineOpt.get();
                if (medicine.getQuantity() < quantity) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Insufficient stock for " + medicine.getName() +
                            ". Available: " + medicine.getQuantity() + ", Requested: " + quantity);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Create or find a walk-in customer user
            User walkInCustomer = createWalkInCustomer(customerData);

            // Get current pharmacist from security context
            SecurityContext securityContext = SecurityContextHolder.getContext();
            User pharmacist = null;
            if (securityContext.getAuthentication() != null && securityContext.getAuthentication().getPrincipal() instanceof UserPrincipal) {
                UserPrincipal authenticatedUser = (UserPrincipal) securityContext.getAuthentication().getPrincipal();
                // map UserPrinciple to User
                pharmacist = userService.getUserById(authenticatedUser.getId())
                        .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            } else {
                // Fallback: get first available pharmacist if no authenticated user
                List<User> pharmacists = userService.getUsersByRole(Role.RoleName.PHARMACIST);
                if (pharmacists.isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "No pharmacist found to handle manual billing");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                pharmacist = pharmacists.get(0);
            }

            // Create a prescription for manual billing (no file upload required)
            Prescription manualPrescription = createManualPrescription(walkInCustomer, pharmacist, items);
            
            // Create bill using the existing BillService and associate with prescription
            Bill savedBill = billService.generateBillFromPrescription(manualPrescription);
            
            // Update payment details since this is immediate payment
            savedBill.setPaymentMethod(Bill.PaymentMethod.valueOf(mapPaymentMethod(paymentMethod)));
            savedBill.setPaymentStatus(Bill.PaymentStatus.PAID);
            savedBill.setPaymentType(Bill.PaymentType.PAY_ON_PICKUP);
            savedBill.setPaidAt(LocalDateTime.now());
            
            String notes = "Manual billing - Walk-in customer: " + customerData.get("name");
            if ("CASH".equals(paymentMethod)) {
                notes += String.format(" | Cash received: $%.2f, Change: $%.2f", receivedAmount, changeAmount);
            }
            savedBill.setNotes(notes);
            
            // Update the bill
            savedBill = billService.updateBill(savedBill);

            // Mark prescription as completed since payment is immediate
            prescriptionService.updateStatus(manualPrescription.getId(), Prescription.PrescriptionStatus.COMPLETED);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put(MESSAGE_KEY, "Manual bill generated successfully");
            response.put("billId", savedBill.getId());
            response.put("billNumber", savedBill.getBillNumber());
            response.put("totalAmount", savedBill.getTotalAmount());
            response.put("paymentMethod", paymentMethod);
            response.put("customerName", customerData.get("name"));
            response.put("itemsCount", items.size());
            response.put("createdAt", savedBill.getCreatedAt());

            if ("CASH".equals(paymentMethod)) {
                response.put("receivedAmount", receivedAmount);
                response.put("changeAmount", changeAmount);
            }

            logger.info("Manual bill generated successfully: {} for customer: {}",
                    savedBill.getBillNumber(), customerData.get("name"));

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            logger.error("Invalid number format in manual billing data: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid number format in bill data");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment method in manual billing: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid payment method: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error generating manual bill: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate manual bill: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Helper method to create a walk-in customer user
     */
    private User createWalkInCustomer(Map<String, String> customerData) {
        User walkInCustomer = new User();
        String customerName = customerData.get("name");
        
        // Split name into first and last name
        String[] nameParts = customerName.split(" ", 2);
        walkInCustomer.setFirstName(nameParts[0]);
        walkInCustomer.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        
        // Set email and phone if provided
        walkInCustomer.setEmail("walkin_" + System.currentTimeMillis() + "@philldesk.com");
        walkInCustomer.setPhone(customerData.get("phone"));
        
        // Set as temporary customer
        walkInCustomer.setUsername("walkin_" + System.currentTimeMillis());
        walkInCustomer.setPassword("TEMP_CUSTOMER"); // Temporary password
        
        // Find existing customer role instead of creating a new one
        Role customerRole = roleService.getRoleByName(Role.RoleName.CUSTOMER)
            .orElseThrow(() -> new RuntimeException("Customer role not found in database"));
        walkInCustomer.setRole(customerRole);
        
        return userService.saveUser(walkInCustomer);
    }

    /**
     * Helper method to create a manual prescription for walk-in customers
     */
    private Prescription createManualPrescription(User customer, User pharmacist, List<Map<String, Object>> items) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber("MAN-P-" + System.currentTimeMillis());
        prescription.setCustomer(customer);
        prescription.setPharmacist(pharmacist);
        prescription.setDoctorName("Walk-in Purchase"); // No doctor for OTC purchases
        prescription.setDoctorLicense("N/A"); // No license for OTC purchases
        prescription.setPrescriptionDate(LocalDateTime.now());
        prescription.setStatus(Prescription.PrescriptionStatus.APPROVED); // Pre-approved for manual billing
        prescription.setNotes("Manual billing - Over-the-counter purchase");
        prescription.setApprovedAt(LocalDateTime.now());
        
        // Save prescription first to get ID
        Prescription savedPrescription = prescriptionService.savePrescription(prescription);
        
        // Create prescription items for each medicine
        Set<PrescriptionItem> prescriptionItems = new HashSet<>();
        for (Map<String, Object> item : items) {
            Long medicineId = ((Number) item.get("medicineId")).longValue();
            Integer quantity = ((Number) item.get("quantity")).intValue();
            
            Medicine medicine = medicineService.getMedicineById(medicineId)
                .orElseThrow(() -> new RuntimeException("Medicine not found: " + medicineId));
            
            PrescriptionItem prescriptionItem = new PrescriptionItem();
            prescriptionItem.setPrescription(savedPrescription);
            prescriptionItem.setMedicine(medicine);
            prescriptionItem.setQuantity(quantity);
            prescriptionItem.setDosage("As directed"); // Default for OTC
            prescriptionItem.setFrequency("As needed"); // Default for OTC
            prescriptionItem.setInstructions("Over-the-counter purchase");
            prescriptionItem.setUnitPrice(medicine.getUnitPrice());
            prescriptionItem.setIsDispensed(true); // Immediately dispensed for manual billing
            
            prescriptionItems.add(prescriptionItem);
            
            // Reduce stock immediately since this is a direct sale
            medicineService.reduceStock(medicineId, quantity);
        }
        
        // Update prescription with items
        savedPrescription.setPrescriptionItems(prescriptionItems);
        return prescriptionService.updatePrescription(savedPrescription);
    }

    /**
     * Helper method to map payment method from frontend to backend enum
     */
    private String mapPaymentMethod(String frontendPaymentMethod) {
        return switch (frontendPaymentMethod.toUpperCase()) {
            case "CASH" -> "CASH";
            case "CREDIT_CARD", "DEBIT_CARD" -> "CARD";
            case "UPI" -> "ONLINE";
            case "CHECK" -> "BANK_TRANSFER";
            default -> "OTHER";
        };
    }

    /**
     * Get manual billing statistics
     */
    @GetMapping("/manual-billing/stats")
    public ResponseEntity<Map<String, Object>> getManualBillingStats() {
        try {
            // Get today's manual bills
            LocalDate today = LocalDate.now();
            List<Bill> allBills = billService.getAllBills();
            
            List<Bill> todayManualBills = allBills.stream()
                .filter(bill -> bill.getBillNumber() != null && bill.getBillNumber().startsWith("MAN-"))
                .filter(bill -> bill.getCreatedAt().toLocalDate().equals(today))
                .toList();

            List<Bill> allManualBills = allBills.stream()
                .filter(bill -> bill.getBillNumber() != null && bill.getBillNumber().startsWith("MAN-"))
                .toList();

            double todayRevenue = todayManualBills.stream()
                .mapToDouble(bill -> bill.getTotalAmount().doubleValue())
                .sum();

            double totalRevenue = allManualBills.stream()
                .mapToDouble(bill -> bill.getTotalAmount().doubleValue())
                .sum();

            Map<String, Object> stats = new HashMap<>();
            stats.put("todayBills", todayManualBills.size());
            stats.put("todayRevenue", todayRevenue);
            stats.put("totalBills", allManualBills.size());
            stats.put("totalRevenue", totalRevenue);
            stats.put("averageBillAmount", allManualBills.isEmpty() ? 0 : totalRevenue / allManualBills.size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching manual billing stats: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch manual billing statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get recent manual bills
     */
    @GetMapping("/manual-billing/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentManualBills(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Bill> allBills = billService.getAllBills();
            
            List<Bill> manualBills = allBills.stream()
                .filter(bill -> bill.getBillNumber() != null && bill.getBillNumber().startsWith("MAN-"))
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt())) // Most recent first
                .limit(limit)
                .toList();

            List<Map<String, Object>> billSummaries = manualBills.stream()
                .map(bill -> {
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("id", bill.getId());
                    summary.put("billNumber", bill.getBillNumber());
                    summary.put("customerName", bill.getCustomer() != null ? 
                        bill.getCustomer().getFirstName() + " " + bill.getCustomer().getLastName() : "Unknown");
                    summary.put("totalAmount", bill.getTotalAmount());
                    summary.put("paymentMethod", bill.getPaymentMethod().toString());
                    summary.put("paymentStatus", bill.getPaymentStatus().toString());
                    summary.put("createdAt", bill.getCreatedAt());
                    return summary;
                })
                .toList();

            return ResponseEntity.ok(billSummaries);
        } catch (Exception e) {
            logger.error("Error fetching recent manual bills: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
