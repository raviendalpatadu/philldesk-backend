package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.BillItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.repository.BillRepository;
import com.philldesk.philldeskbackend.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
    
    private final BillRepository billRepository;
    private final MedicineService medicineService;
    private final PrescriptionService prescriptionService;
    private final BillService billService;
    private final NotificationService notificationService;
    private final MedicineRepository medicineRepository;

    /**
     * Cron job that runs daily at 9:00 AM to check for expired pay-on-pickup bills
     * If a customer has selected pay on pickup and hasn't picked up within 3 days,
     * the stock is restocked and the prescription is rejected
     */
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9:00 AM
    @Transactional
    public void processExpiredPayOnPickupBills() {
        log.info("Starting scheduled task to process expired pay-on-pickup bills");
        
        try {
            // Calculate the cutoff date (3 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);
            
            // Find all pending bills with PAY_ON_PICKUP payment type created before cutoff date
            List<Bill> expiredBills = billRepository.findExpiredPayOnPickupBills(cutoffDate);
            
            log.info("Found {} expired pay-on-pickup bills to process", expiredBills.size());
            
            processBillsList(expiredBills);
            
            log.info("Completed processing {} expired pay-on-pickup bills", expiredBills.size());
            
        } catch (Exception e) {
            log.error("Error in scheduled task for processing expired pay-on-pickup bills: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a list of expired bills
     */
    private void processBillsList(List<Bill> expiredBills) {
        for (Bill bill : expiredBills) {
            try {
                processExpiredBill(bill);
                log.info("Successfully processed expired bill: {}", bill.getBillNumber());
            } catch (Exception e) {
                log.error("Error processing expired bill {}: {}", bill.getBillNumber(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Process a single expired bill by restocking medicines and updating statuses
     */
    private void processExpiredBill(Bill bill) {
        log.info("Processing expired bill: {} created at {}", bill.getBillNumber(), bill.getCreatedAt());
        
        // 1. Restock all medicines from bill items
        restockMedicinesFromBill(bill);
        
        // 2. Update prescription status to REJECTED
        if (bill.getPrescription() != null) {
            Prescription prescription = bill.getPrescription();
            prescription.setStatus(Prescription.PrescriptionStatus.REJECTED);
            prescription.setRejectionReason("Prescription automatically rejected due to non-pickup within 3 days");
            prescription.setUpdatedAt(LocalDateTime.now());
            prescriptionService.savePrescription(prescription);
            log.info("Updated prescription {} status to REJECTED", prescription.getPrescriptionNumber());
        }
        
        // 3. Update bill status to CANCELLED
        bill.setPaymentStatus(Bill.PaymentStatus.CANCELLED);
        bill.setNotes(bill.getNotes() != null ? 
            bill.getNotes() + "; Automatically cancelled due to non-pickup within 3 days" : 
            "Automatically cancelled due to non-pickup within 3 days");
        bill.setUpdatedAt(LocalDateTime.now());
        billService.saveBill(bill);
        
        log.info("Updated bill {} status to CANCELLED", bill.getBillNumber());
    }
    
    /**
     * Restock medicines by adding back the quantities from bill items
     */
    private void restockMedicinesFromBill(Bill bill) {
        if (bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            log.warn("No bill items found for bill: {}", bill.getBillNumber());
            return;
        }
        
        for (BillItem billItem : bill.getBillItems()) {
            try {
                Long medicineId = billItem.getMedicine().getId();
                Integer quantityToRestock = billItem.getQuantity();
                
                // Add the quantity back to medicine stock
                medicineService.increaseStock(medicineId, quantityToRestock);
                
                log.info("Restocked medicine ID {} with quantity {}", medicineId, quantityToRestock);
                
            } catch (Exception e) {
                log.error("Error restocking medicine from bill item {}: {}", billItem.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Manual method to process expired bills (can be called via REST endpoint for testing)
     */
    @Transactional
    public String processExpiredBillsManually() {
        try {
            // Calculate the cutoff date (3 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);
            
            // Find all pending bills with PAY_ON_PICKUP payment type created before cutoff date
            List<Bill> expiredBills = billRepository.findExpiredPayOnPickupBills(cutoffDate);
            
            log.info("Manual processing: Found {} expired pay-on-pickup bills to process", expiredBills.size());
            
            int processedCount = processExpiredBillsWithCount(expiredBills);
            
            return String.format("Successfully processed %d out of %d expired pay-on-pickup bills", 
                processedCount, expiredBills.size());
        } catch (Exception e) {
            log.error("Error in manual processing: {}", e.getMessage(), e);
            return "Error processing expired bills: " + e.getMessage();
        }
    }
    
    /**
     * Process a list of expired bills and return the count of successfully processed bills
     */
    private int processExpiredBillsWithCount(List<Bill> expiredBills) {
        int processedCount = 0;
        for (Bill bill : expiredBills) {
            try {
                processExpiredBill(bill);
                processedCount++;
                log.info("Successfully processed expired bill: {}", bill.getBillNumber());
            } catch (Exception e) {
                log.error("Error processing expired bill {}: {}", bill.getBillNumber(), e.getMessage(), e);
            }
        }
        return processedCount;
    }
    
    /**
     * Get count of bills that will be processed in the next scheduled run
     */
    public long getExpiredBillsCount() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);
        return billRepository.countExpiredPayOnPickupBills(cutoffDate);
    }
    
    /**
     * Cron job that runs daily at 8:00 AM to check for low stock medicines
     * and send notifications to admins and pharmacists
     */
    @Scheduled(cron = "0 0 8 * * *") // Daily at 8:00 AM
    public void checkLowStockMedicines() {
        log.info("Starting scheduled task to check for low stock medicines");
        
        try {
            List<Medicine> lowStockMedicines = medicineRepository.findLowStockMedicines();
            log.info("Found {} medicines with low stock", lowStockMedicines.size());
            
            processLowStockMedicines(lowStockMedicines);
            
            log.info("Completed low stock check for {} medicines", lowStockMedicines.size());
            
        } catch (Exception e) {
            log.error("Error in scheduled task for checking low stock medicines: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process low stock medicines and send notifications
     */
    private void processLowStockMedicines(List<Medicine> lowStockMedicines) {
        for (Medicine medicine : lowStockMedicines) {
            try {
                notificationService.createLowStockNotification(medicine.getId());
                log.info("Created low stock notification for medicine: {} (Current: {}, Reorder: {})", 
                    medicine.getName(), medicine.getQuantity(), medicine.getReorderLevel());
            } catch (Exception e) {
                log.error("Error creating low stock notification for medicine {}: {}", 
                    medicine.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Cron job that runs daily at 8:30 AM to check for medicines expiring within 30 days
     * and send notifications to admins and pharmacists
     */
    @Scheduled(cron = "0 30 8 * * *") // Daily at 8:30 AM
    public void checkExpiringMedicines() {
        log.info("Starting scheduled task to check for expiring medicines");
        
        try {
            // Check medicines expiring within 30 days
            LocalDate cutoffDate = LocalDate.now().plusDays(30);
            List<Medicine> expiringMedicines = medicineRepository.findExpiringMedicines(cutoffDate);
            
            log.info("Found {} medicines expiring within 30 days", expiringMedicines.size());
            
            processExpiringMedicines(expiringMedicines);
            
            log.info("Completed expiry check for {} medicines", expiringMedicines.size());
            
        } catch (Exception e) {
            log.error("Error in scheduled task for checking expiring medicines: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process expiring medicines and send notifications
     */
    private void processExpiringMedicines(List<Medicine> expiringMedicines) {
        for (Medicine medicine : expiringMedicines) {
            try {
                // Only alert for medicines expiring within 30 days but not already expired
                if (medicine.getExpiryDate() != null && 
                    medicine.getExpiryDate().isAfter(LocalDate.now())) {
                    
                    notificationService.createExpiryAlertNotification(medicine.getId());
                    log.info("Created expiry alert notification for medicine: {} (Expiry: {})", 
                        medicine.getName(), medicine.getExpiryDate());
                }
            } catch (Exception e) {
                log.error("Error creating expiry alert notification for medicine {}: {}", 
                    medicine.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Manual method to check low stock medicines (can be called via REST endpoint for testing)
     */
    public String checkLowStockMedicinesManually() {
        try {
            checkLowStockMedicines();
            return "Successfully checked low stock medicines and sent notifications";
        } catch (Exception e) {
            log.error("Error in manual low stock check: {}", e.getMessage(), e);
            return "Error checking low stock medicines: " + e.getMessage();
        }
    }
    
    /**
     * Manual method to check expiring medicines (can be called via REST endpoint for testing)
     */
    public String checkExpiringMedicinesManually() {
        try {
            checkExpiringMedicines();
            return "Successfully checked expiring medicines and sent notifications";
        } catch (Exception e) {
            log.error("Error in manual expiry check: {}", e.getMessage(), e);
            return "Error checking expiring medicines: " + e.getMessage();
        }
    }
    
    /**
     * Get count of low stock medicines
     */
    public long getLowStockMedicinesCount() {
        return medicineRepository.findLowStockMedicines().size();
    }
    
    /**
     * Get count of medicines expiring within 30 days
     */
    public long getExpiringMedicinesCount() {
        LocalDate cutoffDate = LocalDate.now().plusDays(30);
        return medicineRepository.findExpiringMedicines(cutoffDate).size();
    }
}
