/**
 * Analytics Controller
 * 
 * This controller provides comprehensive analytics endpoints specifically
 * for admin analytics dashboard with enhanced data aggregation and insights.
 * Extends the basic dashboard functionality with advanced analytics.
 */

package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.dto.ApiResponse;
import com.philldesk.philldeskbackend.service.BillService;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.PrescriptionItemService;
import com.philldesk.philldeskbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private static final String QUANTITY_KEY = "quantity";
    private static final String REVENUE_KEY = "revenue";
    private static final String SALES_KEY = "sales";

    private final UserService userService;
    private final MedicineService medicineService;
    private final PrescriptionService prescriptionService;
    private final BillService billService;
    private final PrescriptionItemService prescriptionItemService;

    @Autowired
    public AnalyticsController(UserService userService, 
                             MedicineService medicineService,
                             PrescriptionService prescriptionService,
                             BillService billService,
                             PrescriptionItemService prescriptionItemService) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.prescriptionService = prescriptionService;
        this.billService = billService;
        this.prescriptionItemService = prescriptionItemService;
    }

    /**
     * Get comprehensive analytics overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // Basic statistics
            overview.put("totalUsers", userService.getAllUsers().size());
            overview.put("activeUsers", userService.getActiveUsers().size());
            overview.put("totalMedicines", medicineService.getAllMedicines().size());
            overview.put("totalPrescriptions", prescriptionService.getAllPrescriptions().size());
            overview.put("totalBills", billService.getAllBills().size());
            
            // Current month revenue
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = LocalDate.now();
            BigDecimal monthlyRevenue = billService.getTotalRevenue(startOfMonth, endOfMonth);
            overview.put("monthlyRevenue", monthlyRevenue);
            
            return ResponseEntity.ok(ApiResponse.success("Analytics overview retrieved successfully", overview));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve analytics overview: " + e.getMessage()));
        }
    }

    /**
     * Get top selling medications based on recent bills
     */
    @GetMapping("/top-medications")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopMedications(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int limit) {
        
        try {
            LocalDate startDate = LocalDate.now().minusDays(days);
            LocalDate endDate = LocalDate.now();
            
            // Get all prescription items from the specified period
            List<PrescriptionItem> allPrescriptionItems = prescriptionItemService.getAllPrescriptionItems();
            
            // Filter prescription items by date range (using prescription creation date)
            List<PrescriptionItem> recentPrescriptionItems = allPrescriptionItems.stream()
                .filter(item -> {
                    if (item.getPrescription() != null && item.getPrescription().getCreatedAt() != null) {
                        LocalDate itemDate = item.getPrescription().getCreatedAt().toLocalDate();
                        return !itemDate.isBefore(startDate) && !itemDate.isAfter(endDate);
                    }
                    return false;
                })
                .toList();
            
            // Group by medicine and aggregate sales data
            Map<Medicine, Map<String, Object>> medicineAggregation = recentPrescriptionItems.stream()
                .collect(Collectors.groupingBy(
                    PrescriptionItem::getMedicine,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        items -> {
                            int totalQuantity = items.stream().mapToInt(PrescriptionItem::getQuantity).sum();
                            BigDecimal totalRevenue = items.stream()
                                .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                            
                            Map<String, Object> aggregation = new HashMap<>();
                            aggregation.put(QUANTITY_KEY, totalQuantity);
                            aggregation.put(REVENUE_KEY, totalRevenue);
                            return aggregation;
                        }
                    )
                ));
            
            // Convert to list of top medications and sort by quantity sold
            List<Map<String, Object>> topMedications = medicineAggregation.entrySet().stream()
                .map(entry -> {
                    Medicine medicine = entry.getKey();
                    Map<String, Object> aggregation = entry.getValue();
                    
                    // Calculate trend (mock for now - could be implemented with historical data)
                    String trend = calculateTrend((Integer) aggregation.get(QUANTITY_KEY));
                    
                    return createMedicationData(
                        medicine.getName() + " " + (medicine.getStrength() != null ? medicine.getStrength() : ""),
                        (Integer) aggregation.get(QUANTITY_KEY),
                        ((BigDecimal) aggregation.get(REVENUE_KEY)).doubleValue(),
                        trend
                    );
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get(SALES_KEY), (Integer) a.get(SALES_KEY)))
                .toList();
            
            // If no real data found, return a message indicating no sales in the period
            if (topMedications.isEmpty()) {
                topMedications = Arrays.asList(
                    createMedicationData("No sales data", 0, 0.0, "0%")
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success("Top medications retrieved successfully", 
                                                        topMedications.subList(0, Math.min(limit, topMedications.size()))));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve top medications"));
        }
    }

    /**
     * Get sales analytics for a specific period
     */
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> salesData = new HashMap<>();
        
        try {
            List<Bill> bills = billService.getBillsByDateRange(startDate, endDate);
            
            BigDecimal totalRevenue = bills.stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Long totalOrders = (long) bills.size();
            
            BigDecimal avgOrderValue = totalOrders > 0 ? 
                totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            // Group by payment method
            Map<String, Long> paymentMethods = bills.stream()
                .collect(Collectors.groupingBy(
                    bill -> bill.getPaymentMethod().toString(),
                    Collectors.counting()
                ));
            
            salesData.put("totalRevenue", totalRevenue);
            salesData.put("totalOrders", totalOrders);
            salesData.put("averageOrderValue", avgOrderValue);
            salesData.put("paymentMethodBreakdown", paymentMethods);
            salesData.put("period", Map.of("startDate", startDate, "endDate", endDate));
            
            return ResponseEntity.ok(ApiResponse.success("Sales analytics retrieved successfully", salesData));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve sales analytics: " + e.getMessage()));
        }
    }

    /**
     * Get prescription analytics
     */
    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrescriptionAnalytics() {
        Map<String, Object> prescriptionData = new HashMap<>();
        
        try {
            List<Prescription> allPrescriptions = prescriptionService.getAllPrescriptions();
            
            // Status breakdown
            Map<String, Long> statusBreakdown = allPrescriptions.stream()
                .collect(Collectors.groupingBy(
                    prescription -> prescription.getStatus().toString(),
                    Collectors.counting()
                ));
            
            // Today's prescriptions
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
            
            long todaysPrescriptions = allPrescriptions.stream()
                .filter(p -> p.getCreatedAt().isAfter(startOfDay) && p.getCreatedAt().isBefore(endOfDay))
                .count();
            
            // This week's prescriptions
            LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
            long weeklyPrescriptions = allPrescriptions.stream()
                .filter(p -> p.getCreatedAt().isAfter(startOfWeek))
                .count();
            
            prescriptionData.put("total", allPrescriptions.size());
            prescriptionData.put("statusBreakdown", statusBreakdown);
            prescriptionData.put("todaysPrescriptions", todaysPrescriptions);
            prescriptionData.put("weeklyPrescriptions", weeklyPrescriptions);
            
            return ResponseEntity.ok(ApiResponse.success("Prescription analytics retrieved successfully", prescriptionData));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve prescription analytics: " + e.getMessage()));
        }
    }

    /**
     * Get inventory analytics
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryAnalytics() {
        Map<String, Object> inventoryData = new HashMap<>();
        
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
            
            // Category breakdown
            Map<String, Long> categoryBreakdown = allMedicines.stream()
                .filter(m -> m.getCategory() != null)
                .collect(Collectors.groupingBy(
                    Medicine::getCategory,
                    Collectors.counting()
                ));
            
            inventoryData.put("totalItems", totalItems);
            inventoryData.put("inStock", inStock);
            inventoryData.put("lowStock", lowStock);
            inventoryData.put("outOfStock", outOfStock);
            inventoryData.put("totalValue", totalValue);
            inventoryData.put("categoryBreakdown", categoryBreakdown);
            
            // Calculate percentages
            if (totalItems > 0) {
                inventoryData.put("inStockPercentage", Math.round((double) inStock / totalItems * 100));
                inventoryData.put("lowStockPercentage", Math.round((double) lowStock / totalItems * 100));
                inventoryData.put("outOfStockPercentage", Math.round((double) outOfStock / totalItems * 100));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Inventory analytics retrieved successfully", inventoryData));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve inventory analytics: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create medication data
     */
    private Map<String, Object> createMedicationData(String name, int sales, double revenue, String trend) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put(SALES_KEY, sales);
        data.put(REVENUE_KEY, String.format("$%.2f", revenue));
        data.put("trend", trend);
        return data;
    }
    
    /**
     * Helper method to calculate trend based on quantity sold
     * This is a simplified calculation - in a real implementation,
     * you would compare with previous period data
     */
    private String calculateTrend(int quantity) {
        if (quantity > 100) {
            return "+15%";
        } else if (quantity > 50) {
            return "+10%";
        } else if (quantity > 20) {
            return "+5%";
        } else if (quantity > 0) {
            return "+2%";
        } else {
            return "0%";
        }
    }
}
