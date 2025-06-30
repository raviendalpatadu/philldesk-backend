package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    
    List<Medicine> findByIsActiveTrue();
    
    List<Medicine> findByCategory(String category);
    
    List<Medicine> findByManufacturer(String manufacturer);
    
    Optional<Medicine> findByNameAndIsActiveTrue(String name);
    
    @Query("SELECT m FROM Medicine m WHERE m.quantity <= m.reorderLevel AND m.isActive = true")
    List<Medicine> findLowStockMedicines();
    
    @Query("SELECT m FROM Medicine m WHERE m.expiryDate <= :date AND m.isActive = true")
    List<Medicine> findExpiringMedicines(@Param("date") LocalDate date);
    
    @Query("SELECT m FROM Medicine m WHERE m.expiryDate BETWEEN :startDate AND :endDate AND m.isActive = true")
    List<Medicine> findMedicinesExpiringBetween(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    @Query("SELECT m FROM Medicine m WHERE " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND m.isActive = true")
    List<Medicine> searchMedicines(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT m FROM Medicine m WHERE m.isPrescriptionRequired = :requiresPrescription AND m.isActive = true")
    List<Medicine> findByPrescriptionRequirement(@Param("requiresPrescription") boolean requiresPrescription);
    
    @Query("SELECT DISTINCT m.category FROM Medicine m WHERE m.isActive = true ORDER BY m.category")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT m.manufacturer FROM Medicine m WHERE m.isActive = true ORDER BY m.manufacturer")
    List<String> findAllManufacturers();
}
