package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.dto.PrescriptionResponseDTO;
import com.philldesk.philldeskbackend.dto.BillResponseDTO;
import com.philldesk.philldeskbackend.dto.UserResponseDTO;
import com.philldesk.philldeskbackend.security.UserPrincipal;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.BillService;
import com.philldesk.philldeskbackend.service.UserService;
import com.philldesk.philldeskbackend.service.GoogleDriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";

    private final PrescriptionService prescriptionService;
    private final BillService billService;
    private final UserService userService;
    private final GoogleDriveService googleDriveService;

    @Autowired
    public CustomerController(PrescriptionService prescriptionService, 
                             BillService billService,
                             UserService userService,
                             GoogleDriveService googleDriveService) {
        this.prescriptionService = prescriptionService;
        this.billService = billService;
        this.userService = userService;
        this.googleDriveService = googleDriveService;
    }

    /**
     * Get customer dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Long customerId = getCurrentUserId();
            logger.info("Dashboard stats requested by customer ID: {}", customerId);
            
            if (customerId == null) {
                logger.warn("Unauthorized access attempt to dashboard stats");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Map<String, Object> stats = new HashMap<>();
            
            List<Prescription> customerPrescriptions = prescriptionService.getPrescriptionsByCustomerId(customerId);
            stats.put("totalPrescriptions", customerPrescriptions.size());
            
            long pendingCount = customerPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.PENDING)
                .count();
            stats.put("pendingPrescriptions", pendingCount);
            
            long approvedCount = customerPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.APPROVED)
                .count();
            stats.put("approvedPrescriptions", approvedCount);
            
            long readyCount = customerPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.DISPENSED)
                .count();
            stats.put("readyForPickup", readyCount);
            
            long completedCount = customerPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.COMPLETED)
                .count();
            stats.put("completedPrescriptions", completedCount);
            
            long rejectedCount = customerPrescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.REJECTED)
                .count();
            stats.put("rejectedPrescriptions", rejectedCount);

            // Calculate total spent (from completed bills)
            List<Bill> customerBills = billService.getBillsByCustomerId(customerId);
            double totalSpent = customerBills.stream()
                .filter(bill -> bill.getPaymentStatus() == Bill.PaymentStatus.PAID)
                .mapToDouble(bill -> bill.getTotalAmount().doubleValue())
                .sum();
            stats.put("totalSpent", totalSpent);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to retrieve dashboard stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR_KEY, errorResponse));
        }
    }

    /**
     * Upload prescription file
     */
    @PostMapping("/prescriptions/upload")
    public ResponseEntity<Map<String, Object>> uploadPrescription(
            @RequestParam("file") MultipartFile file,
            @RequestParam("doctorName") String doctorName,
            @RequestParam(value = "doctorLicense", required = false) String doctorLicense,
            @RequestParam(value = "notes", required = false) String notes) {
        try {
            Long customerId = getCurrentUserId();
            logger.info("Prescription upload requested by customer ID: {}, file: {}, doctor: {}", 
                       customerId, file.getOriginalFilename(), doctorName);
            
            if (customerId == null) {
                logger.warn("Unauthorized prescription upload attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Validate file
            if (file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Please select a file to upload");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }

            // Check file type
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Invalid file type. Please upload PDF or image files only.");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }

            // Upload file to Google Drive
            String fileUrl = googleDriveService.uploadFile(file);

            // Create prescription record
            Optional<User> customer = userService.getUserById(customerId);
            if (customer.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Customer not found");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }

            Prescription prescription = new Prescription();
            prescription.setCustomer(customer.get());
            prescription.setDoctorName(doctorName);
            prescription.setDoctorLicense(doctorLicense);
            prescription.setFileUrl(fileUrl);
            prescription.setFileName(file.getOriginalFilename());
            prescription.setFileType(getFileExtension(file.getOriginalFilename()));
            prescription.setNotes(notes);
            prescription.setStatus(Prescription.PrescriptionStatus.PENDING);
            prescription.setPrescriptionDate(LocalDateTime.now());
            
            // Generate prescription number
            prescription.setPrescriptionNumber("RX" + System.currentTimeMillis());

            Prescription savedPrescription = prescriptionService.savePrescription(prescription);

            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Prescription uploaded successfully");
            response.put("prescriptionId", savedPrescription.getId());
            response.put("prescriptionNumber", savedPrescription.getPrescriptionNumber());
            response.put("status", savedPrescription.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to upload prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR_KEY, errorResponse));
        }
    }

    /**
     * Get customer's prescriptions
     */
    @GetMapping("/prescriptions")
    public ResponseEntity<List<PrescriptionResponseDTO>> getMyPrescriptions() {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByCustomerId(customerId);
            List<PrescriptionResponseDTO> prescriptionDTOs = prescriptions.stream()
                .map(PrescriptionResponseDTO::fromEntity)
                .toList();
            return ResponseEntity.ok(prescriptionDTOs);
        } catch (Exception e) {
            logger.error("Error retrieving prescriptions for customer {}: {}", getCurrentUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get customer's pending prescriptions
     */
    @GetMapping("/prescriptions/pending")
    public ResponseEntity<List<PrescriptionResponseDTO>> getMyPendingPrescriptions() {
        try {
            Long customerId = getCurrentUserId();
            logger.info("Pending prescriptions requested by customer ID: {}", customerId);
            
            if (customerId == null) {
                logger.warn("Unauthorized access attempt to pending prescriptions");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByCustomerId(customerId);
            List<PrescriptionResponseDTO> pendingPrescriptionDTOs = prescriptions.stream()
                .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.PENDING)
                .map(PrescriptionResponseDTO::fromEntity)
                .toList();
            
            logger.info("Found {} pending prescriptions for customer {}", pendingPrescriptionDTOs.size(), customerId);
            return ResponseEntity.ok(pendingPrescriptionDTOs);
        } catch (Exception e) {
            logger.error("Error retrieving pending prescriptions for customer {}: {}", getCurrentUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific prescription by ID (only if belongs to current customer)
     */
    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionById(@PathVariable Long id) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<Prescription> prescription = prescriptionService.getPrescriptionByIdWithUserDetails(id);
            if (prescription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if prescription belongs to current customer
            if (!prescription.get().getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            PrescriptionResponseDTO prescriptionDTO = PrescriptionResponseDTO.fromEntity(prescription.get());
            return ResponseEntity.ok(prescriptionDTO);
        } catch (Exception e) {
            logger.error("Error retrieving prescription {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get customer's purchase history (bills)
     */
    @GetMapping("/purchase-history")
    public ResponseEntity<List<BillResponseDTO>> getPurchaseHistory() {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Bill> bills = billService.getBillsByCustomerId(customerId);
            List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
            return ResponseEntity.ok(billDTOs);
        } catch (Exception e) {
            logger.error("Error retrieving purchase history for customer {}: {}", getCurrentUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific bill by ID (only if belongs to current customer)
     */
    @GetMapping("/bills/{id}")
    public ResponseEntity<BillResponseDTO> getBillById(@PathVariable Long id) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<Bill> bill = billService.getBillByIdWithDetails(id);
            if (bill.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if bill belongs to current customer
            if (!bill.get().getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            BillResponseDTO billDTO = BillResponseDTO.fromEntity(bill.get());
            return ResponseEntity.ok(billDTO);
        } catch (Exception e) {
            logger.error("Error retrieving bill {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get customer profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile() {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<User> user = userService.getUserById(customerId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            UserResponseDTO userDTO = UserResponseDTO.fromEntity(user.get());
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            logger.error("Error retrieving profile for customer {}: {}", getCurrentUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update customer profile
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody Map<String, Object> profileData) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<User> userOpt = userService.getUserById(customerId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            
            // Update allowed fields
            if (profileData.containsKey("firstName")) {
                user.setFirstName((String) profileData.get("firstName"));
            }
            if (profileData.containsKey("lastName")) {
                user.setLastName((String) profileData.get("lastName"));
            }
            if (profileData.containsKey("phone")) {
                user.setPhone((String) profileData.get("phone"));
            }
            if (profileData.containsKey("address")) {
                user.setAddress((String) profileData.get("address"));
            }

            userService.updateUser(user);

            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Profile updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Download receipt for completed prescription
     */
    @GetMapping("/prescriptions/{id}/receipt")
    public ResponseEntity<Map<String, Object>> downloadReceipt(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            Optional<Prescription> prescriptionOpt = prescriptionService.findById(id);
            if (prescriptionOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Prescription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Prescription prescription = prescriptionOpt.get();
            
            // Verify the prescription belongs to the current user
            if (!prescription.getCustomer().getId().equals(currentUserId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Access denied - prescription does not belong to user");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // Check if prescription is completed
            if (prescription.getStatus() != Prescription.PrescriptionStatus.COMPLETED) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Prescription is not completed yet");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Generate receipt data
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("prescriptionId", prescription.getId());
            receipt.put("prescriptionNumber", prescription.getPrescriptionNumber());
            receipt.put("customerName", prescription.getCustomer().getFirstName() + " " + prescription.getCustomer().getLastName());
            receipt.put("customerEmail", prescription.getCustomer().getEmail());
            receipt.put("doctorName", prescription.getDoctorName());
            receipt.put("completedDate", prescription.getUpdatedAt());
            receipt.put("pharmacistName", prescription.getPharmacist() != null ? 
                prescription.getPharmacist().getFirstName() + " " + prescription.getPharmacist().getLastName() : "Unknown");
            receipt.put("status", prescription.getStatus().toString());
            receipt.put("instructions", prescription.getNotes());
            receipt.put("prescriptionDate", prescription.getPrescriptionDate());
            receipt.put("fileName", prescription.getFileName());

            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            logger.error("Error generating receipt for prescription {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to generate receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get completion details for a prescription
     */
    @GetMapping("/prescriptions/{id}/completion-details")
    public ResponseEntity<Map<String, Object>> getCompletionDetails(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            Optional<Prescription> prescriptionOpt = prescriptionService.findById(id);
            if (prescriptionOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Prescription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Prescription prescription = prescriptionOpt.get();
            
            // Verify the prescription belongs to the current user
            if (!prescription.getCustomer().getId().equals(currentUserId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Access denied - prescription does not belong to user");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            Map<String, Object> completionDetails = prescriptionService.getCompletionDetails(id);
            return ResponseEntity.ok(completionDetails);
        } catch (Exception e) {
            logger.error("Error getting completion details for prescription {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to get completion details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get bills for current customer
     */
    @GetMapping("/bills")
    public ResponseEntity<List<BillResponseDTO>> getMyBills() {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Bill> bills = billService.getBillsByCustomerId(customerId);
            List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .collect(Collectors.toList());

            return ResponseEntity.ok(billDTOs);
        } catch (Exception e) {
            logger.error("Error retrieving bills for customer {}: {}", getCurrentUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update payment type for a bill (online or pay on pickup)
     */
    @PutMapping("/bills/{id}/payment-type")
    public ResponseEntity<Map<String, String>> updatePaymentType(
            @PathVariable Long id,
            @RequestBody Map<String, String> paymentData) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String paymentType = paymentData.get("paymentType");
            if (paymentType == null || (!paymentType.equals("ONLINE") && !paymentType.equals("PAY_ON_PICKUP"))) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Invalid payment type. Must be 'ONLINE' or 'PAY_ON_PICKUP'");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Optional<Bill> billOpt = billService.getBillByIdWithDetails(id);
            if (billOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Bill bill = billOpt.get();
            
            // Check if bill belongs to current customer
            if (!bill.getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if bill is still pending (can't change payment type of paid bills)
            if (bill.getPaymentStatus() != Bill.PaymentStatus.PENDING) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Cannot change payment type for non-pending bills");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            bill.setPaymentType(Bill.PaymentType.valueOf(paymentType));
            billService.updateBill(bill);

            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Payment type updated successfully");
            response.put("paymentType", paymentType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating payment type for bill {}: {}", id, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Failed to update payment type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process online payment for a bill
     */
    @PostMapping("/bills/{id}/pay-online")
    public ResponseEntity<Map<String, Object>> payOnline(
            @PathVariable Long id,
            @RequestBody Map<String, Object> paymentData) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<Bill> billOpt = billService.getBillByIdWithDetails(id);
            if (billOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Bill bill = billOpt.get();
            
            // Check if bill belongs to current customer
            if (!bill.getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if bill is pending and set for online payment
            if (bill.getPaymentStatus() != Bill.PaymentStatus.PENDING) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Bill is not in pending status");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }

            if (bill.getPaymentType() != Bill.PaymentType.ONLINE) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Bill is not set for online payment");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }

            // Process payment (in a real system, this would integrate with payment gateway)
            String paymentMethod = (String) paymentData.get("paymentMethod");
            String cardNumber = (String) paymentData.get("cardNumber");
            String cvv = (String) paymentData.get("cvv");
            String expiryDate = (String) paymentData.get("expiryDate");

            // Simulate payment processing
            boolean paymentSuccessful = processOnlinePayment(paymentMethod, cardNumber, cvv, expiryDate, bill.getTotalAmount());

            if (paymentSuccessful) {
                bill.setPaymentStatus(Bill.PaymentStatus.PAID);
                bill.setPaymentMethod(Bill.PaymentMethod.ONLINE);
                bill.setPaidAt(LocalDateTime.now());
                billService.updateBill(bill);

                Map<String, Object> response = new HashMap<>();
                response.put(MESSAGE_KEY, "Payment processed successfully");
                response.put("billId", bill.getId());
                response.put("amount", bill.getTotalAmount());
                response.put("paymentStatus", "PAID");
                response.put("transactionId", "TXN" + System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR_KEY, "Payment processing failed. Please try again.");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, errorResponse));
            }
        } catch (Exception e) {
            logger.error("Error processing online payment for bill {}: {}", id, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR_KEY, "Payment processing failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR_KEY, errorResponse));
        }
    }

    /**
     * Get payment details for a bill
     */
    @GetMapping("/bills/{id}/payment-details")
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable Long id) {
        try {
            Long customerId = getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<Bill> billOpt = billService.getBillByIdWithDetails(id);
            if (billOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Bill bill = billOpt.get();
            
            // Check if bill belongs to current customer
            if (!bill.getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Map<String, Object> paymentDetails = new HashMap<>();
            paymentDetails.put("billId", bill.getId());
            paymentDetails.put("billNumber", bill.getBillNumber());
            paymentDetails.put("totalAmount", bill.getTotalAmount());
            paymentDetails.put("paymentStatus", bill.getPaymentStatus().toString());
            paymentDetails.put("paymentType", bill.getPaymentType() != null ? bill.getPaymentType().toString() : "PAY_ON_PICKUP");
            paymentDetails.put("paymentMethod", bill.getPaymentMethod() != null ? bill.getPaymentMethod().toString() : null);
            paymentDetails.put("paidAt", bill.getPaidAt());
            paymentDetails.put("createdAt", bill.getCreatedAt());

            return ResponseEntity.ok(paymentDetails);
        } catch (Exception e) {
            logger.error("Error retrieving payment details for bill {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Private helper method to simulate online payment processing
    private boolean processOnlinePayment(String paymentMethod, String cardNumber, String cvv, String expiryDate, BigDecimal amount) {
        // In a real system, this would integrate with payment gateways like Stripe, PayPal, etc.
        // For now, we'll simulate a successful payment
        
        // Basic validation
        if (paymentMethod == null || cardNumber == null || cvv == null || expiryDate == null) {
            return false;
        }
        
        // Simulate some processing time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }

    // Helper methods
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
        } catch (Exception e) {
            logger.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
    }

    private boolean isValidFileType(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("application/pdf") ||
               contentType.startsWith("image/");
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
