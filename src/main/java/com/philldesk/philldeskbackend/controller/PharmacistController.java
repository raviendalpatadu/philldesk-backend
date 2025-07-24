package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.dto.ReadyForPickupPrescriptionDTO;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.entity.Role;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.service.UserService;
import com.philldesk.philldeskbackend.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Autowired
    public PharmacistController(PrescriptionService prescriptionService, 
                               MedicineService medicineService,
                               UserService userService,
                               BillService billService) {
        this.prescriptionService = prescriptionService;
        this.medicineService = medicineService;
        this.userService = userService;
        this.billService = billService;
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
                            response.put(MESSAGE_KEY, "Prescription approved successfully, but no prescription items found. Please add medicines before generating bill.");
                            response.put("status", newStatus.toString());
                            response.put("decision", decision);
                            response.put("warning", "No prescription items - bill not generated");
                            return ResponseEntity.ok(response);
                        }
                        
                        // Check if bill already exists for this prescription
                        Optional<Bill> existingBill = billService.getBillByPrescription(prescription);
                        if (existingBill.isEmpty()) {
                            Bill newBill = billService.generateBillFromPrescription(prescription);
                            Map<String, String> response = new HashMap<>();
                            response.put(MESSAGE_KEY, "Prescription approved successfully and bill created with " + 
                                prescription.getPrescriptionItems().size() + " item(s)");
                            response.put("status", newStatus.toString());
                            response.put("decision", decision);
                            response.put("billId", newBill.getId().toString());
                            response.put("billNumber", newBill.getBillNumber());
                            response.put("billItemsCount", String.valueOf(prescription.getPrescriptionItems().size()));
                            response.put("totalAmount", newBill.getTotalAmount().toString());
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
                        Map<String, String> response = new HashMap<>();
                        response.put(MESSAGE_KEY, "Prescription approved successfully, but bill creation failed: " + billException.getMessage());
                        response.put("status", newStatus.toString());
                        response.put("decision", decision);
                        response.put("warning", "Bill creation failed - please create manually");
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

            // Process the payment
            Bill.PaymentMethod method = Bill.PaymentMethod.valueOf(paymentMethod.toUpperCase());
            billService.markAsPaid(bill.getId(), method);
            
            // Update prescription status to DISPENSED
            prescriptionService.updateStatus(id, Prescription.PrescriptionStatus.DISPENSED);
            
            // Add notes if provided
            if (notes != null && !notes.trim().isEmpty()) {
                bill.setNotes(bill.getNotes() != null ? bill.getNotes() + "; Pickup: " + notes : "Pickup: " + notes);
                billService.updateBill(bill);
            }

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
}
