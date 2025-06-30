package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.service.PrescriptionItemService;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prescription-items")
@CrossOrigin(origins = "*")
public class PrescriptionItemController {

    private final PrescriptionItemService prescriptionItemService;
    private final PrescriptionService prescriptionService;
    private final MedicineService medicineService;

    @Autowired
    public PrescriptionItemController(PrescriptionItemService prescriptionItemService,
                                    PrescriptionService prescriptionService,
                                    MedicineService medicineService) {
        this.prescriptionItemService = prescriptionItemService;
        this.prescriptionService = prescriptionService;
        this.medicineService = medicineService;
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionItem>> getAllPrescriptionItems() {
        List<PrescriptionItem> items = prescriptionItemService.getAllPrescriptionItems();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionItem> getPrescriptionItemById(@PathVariable Long id) {
        Optional<PrescriptionItem> item = prescriptionItemService.getPrescriptionItemById(id);
        return item.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<List<PrescriptionItem>> getPrescriptionItemsByPrescription(@PathVariable Long prescriptionId) {
        List<PrescriptionItem> items = prescriptionItemService.getPrescriptionItemsByPrescriptionId(prescriptionId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<PrescriptionItem>> getPrescriptionItemsByMedicine(@PathVariable Long medicineId) {
        List<PrescriptionItem> items = prescriptionItemService.getPrescriptionItemsByMedicineId(medicineId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<PrescriptionItem> createPrescriptionItem(@RequestBody PrescriptionItem prescriptionItem) {
        try {
            PrescriptionItem savedItem = prescriptionItemService.savePrescriptionItem(prescriptionItem);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionItem> updatePrescriptionItem(@PathVariable Long id, @RequestBody PrescriptionItem prescriptionItem) {
        Optional<PrescriptionItem> existingItem = prescriptionItemService.getPrescriptionItemById(id);
        if (existingItem.isPresent()) {
            prescriptionItem.setId(id);
            PrescriptionItem updatedItem = prescriptionItemService.updatePrescriptionItem(prescriptionItem);
            return ResponseEntity.ok(updatedItem);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescriptionItem(@PathVariable Long id) {
        Optional<PrescriptionItem> existingItem = prescriptionItemService.getPrescriptionItemById(id);
        if (existingItem.isPresent()) {
            prescriptionItemService.deletePrescriptionItem(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/validate-availability")
    public ResponseEntity<Boolean> validateAvailability(@PathVariable Long id) {
        Optional<PrescriptionItem> item = prescriptionItemService.getPrescriptionItemById(id);
        if (item.isPresent()) {
            boolean available = prescriptionItemService.validateAvailability(item.get());
            return ResponseEntity.ok(available);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/prescription/{prescriptionId}/validate-availability")
    public ResponseEntity<List<PrescriptionItem>> validatePrescriptionAvailability(@PathVariable Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
        if (prescription.isPresent()) {
            List<PrescriptionItem> unavailableItems = prescriptionItemService.validatePrescriptionAvailability(prescriptionId);
            return ResponseEntity.ok(unavailableItems);
        }
        return ResponseEntity.notFound().build();
    }
}
