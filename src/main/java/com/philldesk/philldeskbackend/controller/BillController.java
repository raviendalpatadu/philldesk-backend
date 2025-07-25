package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.dto.BillResponseDTO;
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
    public ResponseEntity<List<BillResponseDTO>> getAllBills() {
        List<Bill> bills = billService.getAllBills();
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<BillResponseDTO>> getAllBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Bill> bills = billService.getAllBills(pageable);
        Page<BillResponseDTO> billDTOs = bills.map(BillResponseDTO::fromEntity);
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getBillById(@PathVariable Long id) {
        Optional<Bill> bill = billService.getBillById(id);
        return bill.map(b -> ResponseEntity.ok(BillResponseDTO.fromEntity(b)))
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<BillResponseDTO> getBillByPrescription(@PathVariable Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
        if (prescription.isPresent()) {
            Optional<Bill> bill = billService.getBillByPrescription(prescription.get());
            return bill.map(b -> ResponseEntity.ok(BillResponseDTO.fromEntity(b)))
                      .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BillResponseDTO>> getBillsByCustomer(@PathVariable Long customerId) {
        List<Bill> bills = billService.getBillsByCustomerId(customerId);
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillResponseDTO>> getBillsByStatus(@PathVariable String status) {
        try {
            Bill.PaymentStatus paymentStatus = Bill.PaymentStatus.valueOf(status.toUpperCase());
            List<Bill> bills = billService.getBillsByStatus(paymentStatus);
            List<BillResponseDTO> billDTOs = bills.stream()
                    .map(BillResponseDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(billDTOs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/payment-method/{method}")
    public ResponseEntity<List<BillResponseDTO>> getBillsByPaymentMethod(@PathVariable String method) {
        try {
            Bill.PaymentMethod paymentMethod = Bill.PaymentMethod.valueOf(method.toUpperCase());
            List<Bill> bills = billService.getBillsByPaymentMethod(paymentMethod);
            List<BillResponseDTO> billDTOs = bills.stream()
                    .map(BillResponseDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(billDTOs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<BillResponseDTO>> getBillsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Bill> bills = billService.getBillsByDateRange(startDate, endDate);
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<BillResponseDTO>> getPendingBills() {
        List<Bill> bills = billService.getPendingBills();
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(bill -> BillResponseDTO.fromEntity(bill, true)) // Exclude bill items
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/paid")
    public ResponseEntity<List<BillResponseDTO>> getPaidBills() {
        List<Bill> bills = billService.getPaidBills();
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(bill -> BillResponseDTO.fromEntity(bill, true)) // Exclude bill items
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<BillResponseDTO>> searchBills(@RequestParam String searchTerm) {
        List<Bill> bills = billService.searchBills(searchTerm);
        List<BillResponseDTO> billDTOs = bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(billDTOs);
    }

    @PostMapping
    public ResponseEntity<BillResponseDTO> createBill(@RequestBody Bill bill) {
        try {
            Bill savedBill = billService.saveBill(bill);
            return ResponseEntity.status(HttpStatus.CREATED).body(BillResponseDTO.fromEntity(savedBill));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/generate/{prescriptionId}")
    public ResponseEntity<BillResponseDTO> generateBillFromPrescription(@PathVariable Long prescriptionId) {
        try {
            Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
            if (prescription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if bill already exists
            Optional<Bill> existingBill = billService.getBillByPrescription(prescription.get());
            if (existingBill.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(BillResponseDTO.fromEntity(existingBill.get()));
            }
            
            // Generate new bill
            Bill generatedBill = billService.generateBillFromPrescription(prescription.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(BillResponseDTO.fromEntity(generatedBill));
            
        } catch (IllegalArgumentException e) {
            // Return bad request with error message for validation errors
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Log the error and return internal server error
            System.err.println("Error generating bill for prescription " + prescriptionId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillResponseDTO> updateBill(@PathVariable Long id, @RequestBody Bill bill) {
        Optional<Bill> existingBill = billService.getBillById(id);
        if (existingBill.isPresent()) {
            bill.setId(id);
            Bill updatedBill = billService.updateBill(bill);
            return ResponseEntity.ok(BillResponseDTO.fromEntity(updatedBill));
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

    /**
     * Generate and download invoice PDF
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generateInvoicePDF(@PathVariable Long id) {
        try {
            Optional<Bill> bill = billService.getBillById(id);
            if (bill.isPresent()) {
                byte[] pdfData = billService.generateInvoicePDF(bill.get());
                return ResponseEntity.ok()
                        .header("Content-Type", "application/pdf")
                        .header("Content-Disposition", "attachment; filename=invoice-" + bill.get().getBillNumber() + ".pdf")
                        .body(pdfData);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Print invoice (generate PDF for printing)
     */
    @PostMapping("/{id}/print")
    public ResponseEntity<byte[]> printInvoice(@PathVariable Long id) {
        try {
            Optional<Bill> bill = billService.getBillById(id);
            if (bill.isPresent()) {
                byte[] pdfData = billService.generateInvoicePDF(bill.get());
                return ResponseEntity.ok()
                        .header("Content-Type", "application/pdf")
                        .header("Content-Disposition", "inline; filename=invoice-" + bill.get().getBillNumber() + ".pdf")
                        .body(pdfData);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Email invoice to customer
     */
    @PostMapping("/{id}/email")
    public ResponseEntity<String> emailInvoice(@PathVariable Long id, @RequestParam(required = false) String emailAddress) {
        try {
            Optional<Bill> bill = billService.getBillById(id);
            if (bill.isPresent()) {
                boolean sent = billService.emailInvoice(bill.get(), emailAddress);
                if (sent) {
                    return ResponseEntity.ok("Invoice emailed successfully");
                } else {
                    return ResponseEntity.internalServerError().body("Failed to send email");
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }
    }
}
