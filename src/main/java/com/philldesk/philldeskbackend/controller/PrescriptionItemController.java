package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import com.philldesk.philldeskbackend.service.PrescriptionItemService;
import com.philldesk.philldeskbackend.service.PrescriptionService;
import com.philldesk.philldeskbackend.service.MedicineService;
import com.philldesk.philldeskbackend.dto.PrescriptionItemDTO;
import com.philldesk.philldeskbackend.dto.PrescriptionItemBulkUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

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

    @PostMapping("/prescription/{prescriptionId}/bulk")
    public ResponseEntity<List<PrescriptionItem>> createPrescriptionItemsBulk(
            @PathVariable Long prescriptionId,
            @RequestBody List<PrescriptionItemDTO> prescriptionItemDTOs) {
        try {
            Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
            if (prescription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<PrescriptionItem> prescriptionItems = prescriptionItemDTOs.stream()
                .map(dto -> {
                    PrescriptionItem item = convertToEntity(dto);
                    item.setPrescription(prescription.get());
                    return item;
                })
                .toList();

            List<PrescriptionItem> savedItems = prescriptionItems.stream()
                .map(prescriptionItemService::savePrescriptionItem)
                .toList();

            return ResponseEntity.status(HttpStatus.CREATED).body(savedItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/prescription/{prescriptionId}/bulk")
    public ResponseEntity<List<PrescriptionItem>> updatePrescriptionItemsBulk(
            @PathVariable Long prescriptionId,
            @RequestBody PrescriptionItemBulkUpdateDTO bulkUpdateDTO) {
        try {
            Optional<Prescription> prescription = prescriptionService.getPrescriptionById(prescriptionId);
            if (prescription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Delete existing items for this prescription
            List<PrescriptionItem> existingItems = prescriptionItemService.getPrescriptionItemsByPrescriptionId(prescriptionId);
            existingItems.forEach(item -> prescriptionItemService.deletePrescriptionItem(item.getId()));

            // Create new items
            List<PrescriptionItem> newItems = bulkUpdateDTO.getItems().stream()
                .map(dto -> {
                    PrescriptionItem item = convertToEntity(dto);
                    item.setPrescription(prescription.get());
                    return item;
                })
                .toList();

            List<PrescriptionItem> savedItems = newItems.stream()
                .map(prescriptionItemService::savePrescriptionItem)
                .toList();

            return ResponseEntity.ok(savedItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/prescription/{prescriptionId}/total")
    public ResponseEntity<BigDecimal> calculatePrescriptionTotal(@PathVariable Long prescriptionId) {
        try {
            List<PrescriptionItem> items = prescriptionItemService.getPrescriptionItemsByPrescriptionId(prescriptionId);
            BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private PrescriptionItem convertToEntity(PrescriptionItemDTO dto) {
        PrescriptionItem item = new PrescriptionItem();
        item.setQuantity(dto.getQuantity());
        item.setInstructions(dto.getInstructions());
        item.setUnitPrice(dto.getUnitPrice());
        
        if (dto.getMedicineId() != null) {
            Optional<Medicine> medicine = medicineService.getMedicineById(dto.getMedicineId());
            if (medicine.isPresent()) {
                item.setMedicine(medicine.get());
            } else {
                throw new IllegalArgumentException("Medicine not found with ID: " + dto.getMedicineId());
            }
        }

        return item;
    }
}
