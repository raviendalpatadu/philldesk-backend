package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.*;
import com.philldesk.philldeskbackend.repository.BillRepository;
import com.philldesk.philldeskbackend.repository.UserRepository;
import com.philldesk.philldeskbackend.security.UserPrincipal;
import com.philldesk.philldeskbackend.service.BillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class BillServiceImpl implements BillService {

    private static final Logger logger = LoggerFactory.getLogger(BillServiceImpl.class);
    private final BillRepository billRepository;
    private final UserRepository userRepository;

    @Autowired
    public BillServiceImpl(BillRepository billRepository, UserRepository userRepository) {
        this.billRepository = billRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Bill> getAllBills(Pageable pageable) {
        return billRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bill> getBillByIdWithDetails(Long id) {
        return billRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bill> getBillByPrescription(Prescription prescription) {
        return billRepository.findByPrescription(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByCustomer(User customer) {
        return billRepository.findByCustomer(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByCustomerId(Long customerId) {
        return billRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByStatus(Bill.PaymentStatus status) {
        return billRepository.findByPaymentStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByPaymentMethod(Bill.PaymentMethod paymentMethod) {
        return billRepository.findByPaymentMethod(paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getPendingBills() {
        return billRepository.findByPaymentStatus(Bill.PaymentStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getPaidBills() {
        return billRepository.findByPaymentStatus(Bill.PaymentStatus.PAID);
    }

    @Override
    public Bill generateBillFromPrescription(Prescription prescription) {
        // Validate prescription
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription cannot be null");
        }
        
        if (prescription.getPrescriptionItems() == null || prescription.getPrescriptionItems().isEmpty()) {
            throw new IllegalArgumentException("Prescription must have items to generate a bill");
        }
        
        Bill bill = new Bill();
        bill.setBillNumber(generateBillNumber());
        bill.setPrescription(prescription);
        bill.setCustomer(prescription.getCustomer());
        bill.setPharmacist(prescription.getPharmacist());
        bill.setPaymentStatus(Bill.PaymentStatus.PENDING);
        
        // Create bill items from prescription items
        Set<BillItem> billItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        // Ensure prescription items exist before creating bill items
        if (prescription.getPrescriptionItems() != null && !prescription.getPrescriptionItems().isEmpty()) {
            for (PrescriptionItem prescriptionItem : prescription.getPrescriptionItems()) {
                BillItem billItem = new BillItem();
                billItem.setBill(bill);
                billItem.setMedicine(prescriptionItem.getMedicine());
                billItem.setQuantity(prescriptionItem.getQuantity());
                
                // Use the unit price from prescription item if available, otherwise from medicine
                BigDecimal unitPrice = prescriptionItem.getUnitPrice() != null ? 
                    prescriptionItem.getUnitPrice() : prescriptionItem.getMedicine().getUnitPrice();
                
                // Validate unit price
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Warning: Invalid unit price for medicine: " + prescriptionItem.getMedicine().getName());
                    // Use a default price or skip this item
                    unitPrice = prescriptionItem.getMedicine().getUnitPrice() != null ? 
                        prescriptionItem.getMedicine().getUnitPrice() : BigDecimal.ZERO;
                }
                
                billItem.setUnitPrice(unitPrice);
                
                // Calculate total price for this item
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(prescriptionItem.getQuantity()));
                billItem.setTotalPrice(itemTotal);
                
                // Add prescription item specific details to bill item
                if (prescriptionItem.getInstructions() != null && !prescriptionItem.getInstructions().trim().isEmpty()) {
                    String notes = "Instructions: " + prescriptionItem.getInstructions();
                    if (prescriptionItem.getDosage() != null && !prescriptionItem.getDosage().trim().isEmpty()) {
                        notes += " | Dosage: " + prescriptionItem.getDosage();
                    }
                    if (prescriptionItem.getFrequency() != null && !prescriptionItem.getFrequency().trim().isEmpty()) {
                        notes += " | Frequency: " + prescriptionItem.getFrequency();
                    }
                    billItem.setNotes(notes);
                } else if (prescriptionItem.getDosage() != null && !prescriptionItem.getDosage().trim().isEmpty()) {
                    String notes = "Dosage: " + prescriptionItem.getDosage();
                    if (prescriptionItem.getFrequency() != null && !prescriptionItem.getFrequency().trim().isEmpty()) {
                        notes += " | Frequency: " + prescriptionItem.getFrequency();
                    }
                    billItem.setNotes(notes);
                } else if (prescriptionItem.getFrequency() != null && !prescriptionItem.getFrequency().trim().isEmpty()) {
                    billItem.setNotes("Frequency: " + prescriptionItem.getFrequency());
                }
                
                // Add batch number if available from prescription item or medicine
                if (prescriptionItem.getMedicine().getBatchNumber() != null) {
                    billItem.setBatchNumber(prescriptionItem.getMedicine().getBatchNumber());
                }
                
                billItems.add(billItem);
                subtotal = subtotal.add(itemTotal);
            }
        } else {
            // If no valid prescription items found, throw an exception
            throw new IllegalArgumentException("No valid prescription items found for prescription ID: " + prescription.getId());
        }
        
        // Validate that we have a positive subtotal
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bill subtotal must be greater than zero");
        }
        
        bill.setBillItems(billItems);
        bill.setSubtotal(subtotal);
        bill.setDiscount(BigDecimal.ZERO);
        
        // Calculate tax (10% of subtotal)
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.10"));
        bill.setTax(taxAmount);

        // get pharmacist data from the token
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext.getAuthentication() != null && securityContext.getAuthentication().getPrincipal() instanceof UserPrincipal) {
            UserPrincipal authenticatedUser =  (UserPrincipal) securityContext.getAuthentication().getPrincipal();
            // map UserPrinciple to User
            User user = userRepository.findById(authenticatedUser.getId())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

            bill.setPharmacist(user);
        }
        
        return saveBill(bill);
    }

    @Override
    public Bill saveBill(Bill bill) {
        if (bill.getBillNumber() == null || bill.getBillNumber().isEmpty()) {
            bill.setBillNumber(generateBillNumber());
        }
        
        // Recalculate totals to ensure consistency
        calculateBillTotalsInternal(bill);
        
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        Bill savedBill = billRepository.save(bill);
        
        // Log bill creation for customer notification purposes
        logger.info("Bill created successfully - ID: {}, Number: {}, Customer: {}, Amount: Rs.{}, Prescription: {}", 
            savedBill.getId(), 
            savedBill.getBillNumber(),
            savedBill.getCustomer().getEmail(),
            savedBill.getTotalAmount(),
            savedBill.getPrescription().getPrescriptionNumber()
        );
        
        return savedBill;
    }
    
    /**
     * Recalculate bill totals from bill items to ensure consistency (internal method)
     */
    private void calculateBillTotalsInternal(Bill bill) {
        if (bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            bill.setSubtotal(BigDecimal.ZERO);
            bill.setTotalAmount(BigDecimal.ZERO);
            return;
        }
        
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (BillItem item : bill.getBillItems()) {
            if (item.getTotalPrice() != null) {
                calculatedSubtotal = calculatedSubtotal.add(item.getTotalPrice());
            }
        }
        
        // Update subtotal if it doesn't match calculated value
        bill.setSubtotal(calculatedSubtotal);
        
        // Recalculate tax if needed (ensure it's 10% of subtotal)
        BigDecimal expectedTax = calculatedSubtotal.multiply(new BigDecimal("0.10"));
        if (bill.getTax() == null || !bill.getTax().equals(expectedTax)) {
            bill.setTax(expectedTax);
        }
        
        // The @PrePersist/@PreUpdate method will calculate the final total amount
    }

    @Override
    public Bill updateBill(Bill bill) {
        Optional<Bill> existingBill = billRepository.findById(bill.getId());
        if (existingBill.isPresent()) {
            bill.setCreatedAt(existingBill.get().getCreatedAt());
        }
        bill.setUpdatedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    @Override
    public void deleteBill(Long id) {
        billRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long billId, Bill.PaymentStatus status) {
        Optional<Bill> bill = billRepository.findById(billId);
        if (bill.isPresent()) {
            Bill existingBill = bill.get();
            existingBill.setPaymentStatus(status);
            existingBill.setUpdatedAt(LocalDateTime.now());
            if (status == Bill.PaymentStatus.PAID) {
                existingBill.setPaidAt(LocalDateTime.now());
            }
            billRepository.save(existingBill);
        }
    }

    @Override
    public void markAsPaid(Long billId, Bill.PaymentMethod paymentMethod) {
        Optional<Bill> bill = billRepository.findById(billId);
        if (bill.isPresent()) {
            Bill existingBill = bill.get();
            existingBill.setPaymentStatus(Bill.PaymentStatus.PAID);
            existingBill.setPaymentMethod(paymentMethod);
            existingBill.setPaidAt(LocalDateTime.now());
            existingBill.setUpdatedAt(LocalDateTime.now());
            billRepository.save(existingBill);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(Prescription prescription) {
        BigDecimal subtotal = BigDecimal.ZERO;
        
        if (prescription.getPrescriptionItems() == null || prescription.getPrescriptionItems().isEmpty()) {
            System.out.println("Warning: No prescription items found for prescription ID: " + prescription.getId());
            return subtotal;
        }
        
        for (PrescriptionItem item : prescription.getPrescriptionItems()) {
            // Use consistent pricing logic - same as in generateBillFromPrescription
            BigDecimal unitPrice = item.getUnitPrice() != null ? 
                item.getUnitPrice() : item.getMedicine().getUnitPrice();
                
            // Validate unit price
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Warning: Invalid unit price for medicine: " + item.getMedicine().getName());
                continue;
            }
            
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }
        
        // Calculate tax (10% of subtotal) and return total with tax
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.10"));
        return subtotal.add(taxAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> searchBills(String searchTerm) {
        return billRepository.searchBills(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.getTotalRevenueBetween(startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalBillCount(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.countByCreatedAtBetween(startDateTime, endDateTime);
    }

    private String generateBillNumber() {
        // Generate unique bill number with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "BILL-" + timestamp;
    }

    @Override
    public byte[] generateInvoicePDF(Bill bill) {
        try {
            // Create a simple HTML-based PDF content
            StringBuilder htmlContent = new StringBuilder();
            
            htmlContent.append("<!DOCTYPE html>");
            htmlContent.append("<html><head>");
            htmlContent.append("<style>");
            htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            htmlContent.append(".header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }");
            htmlContent.append(".invoice-details { display: flex; justify-content: space-between; margin-bottom: 30px; }");
            htmlContent.append(".customer-info, .pharmacy-info { width: 45%; }");
            htmlContent.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
            htmlContent.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
            htmlContent.append("th { background-color: #f2f2f2; }");
            htmlContent.append(".total-section { text-align: right; margin-top: 20px; }");
            htmlContent.append(".total-amount { font-size: 18px; font-weight: bold; color: #333; }");
            htmlContent.append("</style>");
            htmlContent.append("</head><body>");
            
            // Header
            htmlContent.append("<div class='header'>");
            htmlContent.append("<h1>PhillDesk Pharmacy</h1>");
            htmlContent.append("<h2>INVOICE</h2>");
            htmlContent.append("</div>");
            
            // Invoice and customer details
            htmlContent.append("<div class='invoice-details'>");
            htmlContent.append("<div class='pharmacy-info'>");
            htmlContent.append("<h3>Pharmacy Information</h3>");
            htmlContent.append("<p><strong>PhillDesk Pharmacy</strong><br>");
            htmlContent.append("123 Healthcare Street<br>");
            htmlContent.append("Medical District<br>");
            htmlContent.append("City, State 12345<br>");
            htmlContent.append("Phone: (555) 123-4567</p>");
            htmlContent.append("</div>");
            htmlContent.append("<div class='customer-info'>");
            htmlContent.append("<h3>Bill To</h3>");
            htmlContent.append("<p><strong>").append(bill.getCustomer().getFirstName()).append(" ").append(bill.getCustomer().getLastName()).append("</strong><br>");
            if (bill.getCustomer().getPhone() != null) {
                htmlContent.append("Phone: ").append(bill.getCustomer().getPhone()).append("<br>");
            }
            if (bill.getCustomer().getEmail() != null) {
                htmlContent.append("Email: ").append(bill.getCustomer().getEmail()).append("<br>");
            }
            htmlContent.append("</p>");
            htmlContent.append("<p><strong>Invoice #:</strong> ").append(bill.getBillNumber()).append("<br>");
            htmlContent.append("<strong>Date:</strong> ").append(bill.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("<br>");
            if (bill.getPrescription() != null) {
                htmlContent.append("<strong>Prescription #:</strong> ").append(bill.getPrescription().getPrescriptionNumber()).append("<br>");
            }
            htmlContent.append("</p>");
            htmlContent.append("</div>");
            htmlContent.append("</div>");
            
            // Items table
            htmlContent.append("<table>");
            htmlContent.append("<thead>");
            htmlContent.append("<tr>");
            htmlContent.append("<th>Item Description</th>");
            htmlContent.append("<th>Quantity</th>");
            htmlContent.append("<th>Unit Price</th>");
            htmlContent.append("<th>Total</th>");
            htmlContent.append("</tr>");
            htmlContent.append("</thead>");
            htmlContent.append("<tbody>");
            
            if (bill.getBillItems() != null && !bill.getBillItems().isEmpty()) {
                for (BillItem item : bill.getBillItems()) {
                    htmlContent.append("<tr>");
                    // Use medicine name as description
                    String description = item.getMedicine() != null ? item.getMedicine().getName() : "Unknown Item";
                    if (item.getBatchNumber() != null) {
                        description += " (Batch: " + item.getBatchNumber() + ")";
                    }
                    htmlContent.append("<td>").append(description).append("</td>");
                    htmlContent.append("<td>").append(item.getQuantity()).append("</td>");
                    htmlContent.append("<td>Rs. ").append(item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0.00").append("</td>");
                    htmlContent.append("<td>Rs. ").append(item.getTotalPrice() != null ? item.getTotalPrice().toString() : "0.00").append("</td>");
                    htmlContent.append("</tr>");
                }
            } else {
                htmlContent.append("<tr>");
                htmlContent.append("<td colspan='4'>No items available</td>");
                htmlContent.append("</tr>");
            }
            
            htmlContent.append("</tbody>");
            htmlContent.append("</table>");
            
            // Total section
            htmlContent.append("<div class='total-section'>");
            htmlContent.append("<p><strong>Subtotal: Rs. ").append(bill.getSubtotal() != null ? bill.getSubtotal().toString() : "0.00").append("</strong></p>");
            if (bill.getTax() != null && bill.getTax().compareTo(BigDecimal.ZERO) > 0) {
                htmlContent.append("<p><strong>Tax: Rs. ").append(bill.getTax().toString()).append("</strong></p>");
            }
            if (bill.getDiscount() != null && bill.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                htmlContent.append("<p><strong>Discount: -Rs. ").append(bill.getDiscount().toString()).append("</strong></p>");
            }
            htmlContent.append("<p class='total-amount'><strong>Total Amount: Rs. ").append(bill.getTotalAmount() != null ? bill.getTotalAmount().toString() : "0.00").append("</strong></p>");
            htmlContent.append("<p><strong>Payment Status: ").append(bill.getPaymentStatus().toString()).append("</strong></p>");
            if (bill.getPaymentMethod() != null) {
                htmlContent.append("<p><strong>Payment Method: ").append(bill.getPaymentMethod().toString()).append("</strong></p>");
            }
            htmlContent.append("</div>");
            
            // Footer
            htmlContent.append("<div style='margin-top: 50px; text-align: center; font-size: 12px; color: #666;'>");
            htmlContent.append("<p>Thank you for choosing PhillDesk Pharmacy!</p>");
            htmlContent.append("<p>For any questions regarding this invoice, please contact us at (555) 123-4567</p>");
            htmlContent.append("</div>");
            
            htmlContent.append("</body></html>");
            
            // For now, return the HTML content as bytes
            // In a real implementation, you would use a library like iText or Flying Saucer to convert HTML to PDF
            return htmlContent.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    @Override
    public boolean emailInvoice(Bill bill, String emailAddress) {
        try {
            // Use the customer's email if no specific email provided
            String targetEmail = emailAddress != null ? emailAddress : bill.getCustomer().getEmail();
            
            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                throw new RuntimeException("No email address available");
            }
            
            // Generate PDF content
            byte[] pdfData = generateInvoicePDF(bill);
            
            // Create email content
            String subject = "Invoice from PhillDesk Pharmacy - " + bill.getBillNumber();
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Dear ").append(bill.getCustomer().getFirstName()).append(" ").append(bill.getCustomer().getLastName()).append(",\n\n");
            emailBody.append("Please find attached your invoice from PhillDesk Pharmacy.\n\n");
            emailBody.append("Invoice Details:\n");
            emailBody.append("Invoice Number: ").append(bill.getBillNumber()).append("\n");
            emailBody.append("Date: ").append(bill.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("\n");
            emailBody.append("Total Amount: Rs. ").append(bill.getTotalAmount().toString()).append("\n");
            emailBody.append("Payment Status: ").append(bill.getPaymentStatus().toString()).append("\n\n");
            if (bill.getPrescription() != null) {
                emailBody.append("Prescription Number: ").append(bill.getPrescription().getPrescriptionNumber()).append("\n\n");
            }
            emailBody.append("Thank you for choosing PhillDesk Pharmacy!\n\n");
            emailBody.append("Best regards,\n");
            emailBody.append("PhillDesk Pharmacy Team\n");
            emailBody.append("Phone: (555) 123-4567\n");
            emailBody.append("Email: info@philldesk.com");
            
            // Log the email sending (in a real implementation, integrate with email service)
            System.out.println("Email would be sent to: " + targetEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + emailBody.toString());
            System.out.println("PDF attachment size: " + pdfData.length + " bytes");
            
            // For demo purposes, always return true
            // In production, integrate with actual email service like SendGrid, AWS SES, etc.
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean validateBillCalculations(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return false;
        }
        
        // Calculate expected subtotal from bill items
        BigDecimal expectedSubtotal = BigDecimal.ZERO;
        for (BillItem item : bill.getBillItems()) {
            if (item.getTotalPrice() != null) {
                expectedSubtotal = expectedSubtotal.add(item.getTotalPrice());
            }
        }
        
        // Check if subtotal matches
        if (bill.getSubtotal() == null || !bill.getSubtotal().equals(expectedSubtotal)) {
            System.out.println("Bill subtotal mismatch. Expected: " + expectedSubtotal + ", Actual: " + bill.getSubtotal());
            return false;
        }
        
        // Check tax calculation (should be 10% of subtotal)
        BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal("0.10"));
        if (bill.getTax() == null || !bill.getTax().equals(expectedTax)) {
            System.out.println("Bill tax mismatch. Expected: " + expectedTax + ", Actual: " + bill.getTax());
            return false;
        }
        
        // Check total amount calculation
        BigDecimal discountAmount = bill.getDiscount() != null ? bill.getDiscount() : BigDecimal.ZERO;
        BigDecimal expectedTotal = expectedSubtotal.subtract(discountAmount).add(expectedTax);
        if (bill.getTotalAmount() == null || !bill.getTotalAmount().equals(expectedTotal)) {
            System.out.println("Bill total mismatch. Expected: " + expectedTotal + ", Actual: " + bill.getTotalAmount());
            return false;
        }
        
        return true;
    }
    
    @Override
    public void recalculateBillTotals(Bill bill) {
        if (bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            bill.setSubtotal(BigDecimal.ZERO);
            bill.setTotalAmount(BigDecimal.ZERO);
            return;
        }
        
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (BillItem item : bill.getBillItems()) {
            if (item.getTotalPrice() != null) {
                calculatedSubtotal = calculatedSubtotal.add(item.getTotalPrice());
            }
        }
        
        // Update subtotal
        bill.setSubtotal(calculatedSubtotal);
        
        // Recalculate tax (10% of subtotal)
        BigDecimal expectedTax = calculatedSubtotal.multiply(new BigDecimal("0.10"));
        bill.setTax(expectedTax);
        
        // The @PrePersist/@PreUpdate method will calculate the final total amount
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getOnlinePaidBillsByPharmacistWithShipping(Long pharmacistId) {
        return billRepository.findOnlinePaidBillsByPharmacistWithShipping(pharmacistId);
    }
}
