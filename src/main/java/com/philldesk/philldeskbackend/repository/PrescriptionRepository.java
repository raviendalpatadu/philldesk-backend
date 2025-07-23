package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    List<Prescription> findByCustomer(User customer);
    
    List<Prescription> findByPharmacist(User pharmacist);
    
    List<Prescription> findByStatus(Prescription.PrescriptionStatus status);
    
    List<Prescription> findByCustomerAndStatus(User customer, Prescription.PrescriptionStatus status);
    
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);
    
    @Query("SELECT p FROM Prescription p WHERE p.status = :status ORDER BY p.createdAt ASC")
    List<Prescription> findByStatusOrderByCreatedAt(@Param("status") Prescription.PrescriptionStatus status);
    
    @Query("SELECT p FROM Prescription p WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC")
    List<Prescription> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId);
    
    @Query("SELECT p FROM Prescription p JOIN FETCH p.customer LEFT JOIN FETCH p.pharmacist WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC")
    List<Prescription> findByCustomerIdWithUserDetailsOrderByCreatedAtDesc(@Param("customerId") Long customerId);
    
    @Query("SELECT p FROM Prescription p JOIN FETCH p.customer LEFT JOIN FETCH p.pharmacist WHERE p.id = :id")
    Optional<Prescription> findByIdWithUserDetails(@Param("id") Long id);
    
    @Query("SELECT p FROM Prescription p WHERE p.pharmacist.id = :pharmacistId ORDER BY p.createdAt DESC")
    List<Prescription> findByPharmacistIdOrderByCreatedAtDesc(@Param("pharmacistId") Long pharmacistId);
    
    @Query("SELECT p FROM Prescription p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Prescription> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Prescription p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Prescription> findPendingPrescriptionsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.status = :status")
    Long countByStatus(@Param("status") Prescription.PrescriptionStatus status);
    
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.customer.id = :customerId")
    Long countByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.pharmacist.id = :pharmacistId")
    Long countByPharmacistId(@Param("pharmacistId") Long pharmacistId);
    
    @Query("SELECT p FROM Prescription p JOIN FETCH p.customer LEFT JOIN FETCH p.pharmacist ORDER BY p.createdAt DESC")
    List<Prescription> findAllWithUserDetailsOrderByCreatedAtDesc();
    
    @Query("SELECT p FROM Prescription p WHERE " +
           "(LOWER(p.prescriptionNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.doctorName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Prescription> searchPrescriptions(@Param("searchTerm") String searchTerm);
}
