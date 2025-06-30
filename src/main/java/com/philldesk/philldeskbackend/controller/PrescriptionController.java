package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserService userService;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService, UserService userService) {
        this.prescriptionService = prescriptionService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Prescription>> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Prescription>> getAllPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Prescription> prescriptions = prescriptionService.getAllPrescriptions(pageable);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(id);
        return prescription.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByCustomer(@PathVariable Long customerId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByCustomerId(customerId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/pharmacist/{pharmacistId}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByPharmacist(@PathVariable Long pharmacistId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPharmacistId(pharmacistId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByStatus(@PathVariable String status) {
        try {
            Prescription.PrescriptionStatus prescriptionStatus = Prescription.PrescriptionStatus.valueOf(status.toUpperCase());
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByStatus(prescriptionStatus);
            return ResponseEntity.ok(prescriptions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Prescription>> getPrescriptionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Prescription>> getPendingPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getPendingPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/processing")
    public ResponseEntity<List<Prescription>> getProcessingPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getProcessingPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Prescription>> getCompletedPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getCompletedPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Prescription>> searchPrescriptions(@RequestParam String searchTerm) {
        List<Prescription> prescriptions = prescriptionService.searchPrescriptions(searchTerm);
        return ResponseEntity.ok(prescriptions);
    }

    @PostMapping
    public ResponseEntity<Prescription> createPrescription(@RequestBody Prescription prescription) {
        try {
            Prescription savedPrescription = prescriptionService.savePrescription(prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPrescription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prescription> updatePrescription(@PathVariable Long id, @RequestBody Prescription prescription) {
        Optional<Prescription> existingPrescription = prescriptionService.getPrescriptionById(id);
        if (existingPrescription.isPresent()) {
            prescription.setId(id);
            Prescription updatedPrescription = prescriptionService.updatePrescription(prescription);
            return ResponseEntity.ok(updatedPrescription);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        Optional<Prescription> existingPrescription = prescriptionService.getPrescriptionById(id);
        if (existingPrescription.isPresent()) {
            prescriptionService.deletePrescription(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Prescription.PrescriptionStatus prescriptionStatus = Prescription.PrescriptionStatus.valueOf(status.toUpperCase());
            prescriptionService.updateStatus(id, prescriptionStatus);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/assign-pharmacist")
    public ResponseEntity<Void> assignPharmacist(@PathVariable Long id, @RequestParam Long pharmacistId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(id);
        Optional<User> pharmacist = userService.getUserById(pharmacistId);
        
        if (prescription.isPresent() && pharmacist.isPresent()) {
            prescriptionService.assignPharmacist(id, pharmacistId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/can-fulfill")
    public ResponseEntity<Boolean> canBeFulfilled(@PathVariable Long id) {
        boolean canFulfill = prescriptionService.canBeFulfilled(id);
        return ResponseEntity.ok(canFulfill);
    }
}
