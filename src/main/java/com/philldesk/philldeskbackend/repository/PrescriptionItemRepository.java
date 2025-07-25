package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.entity.PrescriptionItem;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    
    List<PrescriptionItem> findByPrescription(Prescription prescription);
    
    List<PrescriptionItem> findByMedicine(Medicine medicine);
    
    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
    
    List<PrescriptionItem> findByMedicineId(Long medicineId);
    
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.prescription.id = :prescriptionId")
    List<PrescriptionItem> findItemsByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
    
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.isDispensed = :dispensed")
    List<PrescriptionItem> findByDispensedStatus(@Param("dispensed") boolean dispensed);
    
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.prescription.id = :prescriptionId AND pi.isDispensed = false")
    List<PrescriptionItem> findUndispensedItemsByPrescription(@Param("prescriptionId") Long prescriptionId);
    
    @Query("SELECT SUM(pi.totalPrice) FROM PrescriptionItem pi WHERE pi.prescription.id = :prescriptionId")
    Double calculateTotalAmountByPrescription(@Param("prescriptionId") Long prescriptionId);
    
    @Modifying
    @Query("DELETE FROM PrescriptionItem pi WHERE pi.prescription.id = :prescriptionId")
    void deleteByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
