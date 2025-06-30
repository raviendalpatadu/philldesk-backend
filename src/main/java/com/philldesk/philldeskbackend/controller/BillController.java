package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.service.BillService;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class BillController {

    private final BillService billService;
    private final PrescriptionService prescriptionService;

    @Autowired
    public BillController(BillService billService, PrescriptionService prescriptionService) {
        this.billService = billService;
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getAllBills() {
        List<Bill> bills = billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Bill>> getAllBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Bill> bills = billService.getAllBills(pageable);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bill> getBillById(@PathVariable Long id) {
        Optional<Bill> bill = billService.getBillById(id);
        return bill.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<Bill> getBillByPrescription(@PathVariable Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
        if (prescription.isPresent()) {
            Optional<Bill> bill = billService.getBillByPrescription(prescription.get());
            return bill.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Bill>> getBillsByCustomer(@PathVariable Long customerId) {
        List<Bill> bills = billService.getBillsByCustomerId(customerId);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Bill>> getBillsByStatus(@PathVariable String status) {
        try {
            Bill.PaymentStatus paymentStatus = Bill.PaymentStatus.valueOf(status.toUpperCase());
            List<Bill> bills = billService.getBillsByStatus(paymentStatus);
            return ResponseEntity.ok(bills);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/payment-method/{method}")
    public ResponseEntity<List<Bill>> getBillsByPaymentMethod(@PathVariable String method) {
        try {
            Bill.PaymentMethod paymentMethod = Bill.PaymentMethod.valueOf(method.toUpperCase());
            List<Bill> bills = billService.getBillsByPaymentMethod(paymentMethod);
            return ResponseEntity.ok(bills);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Bill>> getBillsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Bill> bills = billService.getBillsByDateRange(startDate, endDate);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Bill>> getPendingBills() {
        List<Bill> bills = billService.getPendingBills();
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/paid")
    public ResponseEntity<List<Bill>> getPaidBills() {
        List<Bill> bills = billService.getPaidBills();
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Bill>> searchBills(@RequestParam String searchTerm) {
        List<Bill> bills = billService.searchBills(searchTerm);
        return ResponseEntity.ok(bills);
    }

    @PostMapping
    public ResponseEntity<Bill> createBill(@RequestBody Bill bill) {
        try {
            Bill savedBill = billService.saveBill(bill);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/generate/{prescriptionId}")
    public ResponseEntity<Bill> generateBillFromPrescription(@PathVariable Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
        if (prescription.isPresent()) {
            try {
                Bill generatedBill = billService.generateBillFromPrescription(prescription.get());
                return ResponseEntity.status(HttpStatus.CREATED).body(generatedBill);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bill> updateBill(@PathVariable Long id, @RequestBody Bill bill) {
        Optional<Bill> existingBill = billService.getBillById(id);
        if (existingBill.isPresent()) {
            bill.setId(id);
            Bill updatedBill = billService.updateBill(bill);
            return ResponseEntity.ok(updatedBill);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        Optional<Bill> existingBill = billService.getBillById(id);
        if (existingBill.isPresent()) {
            billService.deleteBill(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Bill.PaymentStatus paymentStatus = Bill.PaymentStatus.valueOf(status.toUpperCase());
            billService.updateStatus(id, paymentStatus);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markAsPaid(@PathVariable Long id, @RequestParam String paymentMethod) {
        try {
            Bill.PaymentMethod method = Bill.PaymentMethod.valueOf(paymentMethod.toUpperCase());
            billService.markAsPaid(id, method);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/calculate/{prescriptionId}")
    public ResponseEntity<BigDecimal> calculateTotalAmount(@PathVariable Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
        if (prescription.isPresent()) {
            BigDecimal totalAmount = billService.calculateTotalAmount(prescription.get());
            return ResponseEntity.ok(totalAmount);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal revenue = billService.getTotalRevenue(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalBillCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long count = billService.getTotalBillCount(startDate, endDate);
        return ResponseEntity.ok(count);
    }
}
