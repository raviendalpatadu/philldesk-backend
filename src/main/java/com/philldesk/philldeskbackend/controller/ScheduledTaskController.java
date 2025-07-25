package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scheduled-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('PHARMACIST')")
@Slf4j
public class ScheduledTaskController {

    private final ScheduledTaskService scheduledTaskService;

    /**
     * Manually trigger the expired pay-on-pickup bills processing
     * Useful for testing or immediate processing
     */
    @PostMapping("/process-expired-bills")
    public ResponseEntity<String> processExpiredBillsManually() {
        log.info("Manual trigger for processing expired pay-on-pickup bills requested");
        
        try {
            String result = scheduledTaskService.processExpiredBillsManually();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in manual processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error processing expired bills: " + e.getMessage());
        }
    }

    /**
     * Get the count of bills that would be processed if the scheduled task runs now
     */
    @GetMapping("/expired-bills-count")
    public ResponseEntity<Long> getExpiredBillsCount() {
        try {
            long count = scheduledTaskService.getExpiredBillsCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting expired bills count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    /**
     * Manually trigger the low stock medicines check
     */
    @PostMapping("/check-low-stock")
    public ResponseEntity<String> checkLowStockMedicinesManually() {
        log.info("Manual trigger for checking low stock medicines requested");
        
        try {
            String result = scheduledTaskService.checkLowStockMedicinesManually();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in manual low stock check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error checking low stock medicines: " + e.getMessage());
        }
    }

    /**
     * Manually trigger the expiring medicines check
     */
    @PostMapping("/check-expiring-medicines")
    public ResponseEntity<String> checkExpiringMedicinesManually() {
        log.info("Manual trigger for checking expiring medicines requested");
        
        try {
            String result = scheduledTaskService.checkExpiringMedicinesManually();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in manual expiry check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error checking expiring medicines: " + e.getMessage());
        }
    }

    /**
     * Get the count of low stock medicines
     */
    @GetMapping("/low-stock-count")
    public ResponseEntity<Long> getLowStockMedicinesCount() {
        try {
            long count = scheduledTaskService.getLowStockMedicinesCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting low stock medicines count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    /**
     * Get the count of medicines expiring within 30 days
     */
    @GetMapping("/expiring-medicines-count")
    public ResponseEntity<Long> getExpiringMedicinesCount() {
        try {
            long count = scheduledTaskService.getExpiringMedicinesCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting expiring medicines count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    /**
     * Health check endpoint for scheduled tasks
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Scheduled task service is running");
    }
}
