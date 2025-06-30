package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.dto.ApiResponse;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final UserService userService;
    private final MedicineService medicineService;
    private final PrescriptionService prescriptionService;
    private final BillService billService;
    private final NotificationService notificationService;

    @Autowired
    public DashboardController(UserService userService, 
                             MedicineService medicineService,
                             PrescriptionService prescriptionService,
                             BillService billService,
                             NotificationService notificationService) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.prescriptionService = prescriptionService;
        this.billService = billService;
        this.notificationService = notificationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        List<User> allUsers = userService.getAllUsers();
        List<User> activeUsers = userService.getActiveUsers();
        stats.put("totalUsers", allUsers.size());
        stats.put("activeUsers", activeUsers.size());
        
        // Medicine statistics
        List<Medicine> allMedicines = medicineService.getAllMedicines();
        List<Medicine> availableMedicines = medicineService.getAvailableMedicines();
        List<Medicine> lowStockMedicines = medicineService.getLowStockMedicines(10);
        stats.put("totalMedicines", allMedicines.size());
        stats.put("availableMedicines", availableMedicines.size());
        stats.put("lowStockMedicines", lowStockMedicines.size());
        
        // Prescription statistics
        List<Prescription> pendingPrescriptions = prescriptionService.getPendingPrescriptions();
        List<Prescription> processingPrescriptions = prescriptionService.getProcessingPrescriptions();
        List<Prescription> completedPrescriptions = prescriptionService.getCompletedPrescriptions();
        stats.put("pendingPrescriptions", pendingPrescriptions.size());
        stats.put("processingPrescriptions", processingPrescriptions.size());
        stats.put("completedPrescriptions", completedPrescriptions.size());
        
        // Bill statistics
        List<Bill> pendingBills = billService.getPendingBills();
        List<Bill> paidBills = billService.getPaidBills();
        stats.put("pendingBills", pendingBills.size());
        stats.put("paidBills", paidBills.size());
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> revenueStats = new HashMap<>();
        
        BigDecimal totalRevenue = billService.getTotalRevenue(startDate, endDate);
        Long totalBills = billService.getTotalBillCount(startDate, endDate);
        
        revenueStats.put("totalRevenue", totalRevenue);
        revenueStats.put("totalBills", totalBills);
        revenueStats.put("averageOrderValue", totalBills > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalBills)) : BigDecimal.ZERO);
        revenueStats.put("startDate", startDate);
        revenueStats.put("endDate", endDate);
        
        return ResponseEntity.ok(ApiResponse.success("Revenue statistics retrieved successfully", revenueStats));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemAlerts() {
        Map<String, Object> alerts = new HashMap<>();
        
        // Low stock alerts
        List<Medicine> lowStockMedicines = medicineService.getLowStockMedicines(10);
        alerts.put("lowStockMedicines", lowStockMedicines);
        
        // Pending prescriptions that need attention
        List<Prescription> pendingPrescriptions = prescriptionService.getPendingPrescriptions();
        alerts.put("pendingPrescriptions", pendingPrescriptions);
        
        // Unpaid bills
        List<Bill> pendingBills = billService.getPendingBills();
        alerts.put("pendingBills", pendingBills);
        
        return ResponseEntity.ok(ApiResponse.success("System alerts retrieved successfully", alerts));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();
        
        // Get recent prescriptions (last 7 days)
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        LocalDate today = LocalDate.now();
        
        List<Prescription> recentPrescriptions = prescriptionService.getPrescriptionsByDateRange(weekAgo, today);
        List<Bill> recentBills = billService.getBillsByDateRange(weekAgo, today);
        
        activity.put("recentPrescriptions", recentPrescriptions);
        activity.put("recentBills", recentBills);
        activity.put("period", "Last 7 days");
        
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved successfully", activity));
    }

    @GetMapping("/user/{userId}/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount(@PathVariable Long userId) {
        Long unreadCount = notificationService.getUnreadCountForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread notification count retrieved successfully", unreadCount));
    }

    @GetMapping("/medicines/expiring")
    public ResponseEntity<ApiResponse<List<Medicine>>> getExpiringMedicines(
            @RequestParam(defaultValue = "30") int days) {
        // Note: This would require implementing the expiring medicines query in the repository
        // For now, we'll return available medicines as a placeholder
        List<Medicine> medicines = medicineService.getAvailableMedicines();
        return ResponseEntity.ok(ApiResponse.success("Expiring medicines retrieved successfully", medicines));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Quick counts
        summary.put("totalMedicines", medicineService.getAllMedicines().size());
        summary.put("pendingPrescriptions", prescriptionService.getPendingPrescriptions().size());
        summary.put("lowStockAlerts", medicineService.getLowStockMedicines(10).size());
        summary.put("activeUsers", userService.getActiveUsers().size());
        
        // Today's revenue
        LocalDate today = LocalDate.now();
        BigDecimal todayRevenue = billService.getTotalRevenue(today, today);
        summary.put("todayRevenue", todayRevenue);
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }
}
