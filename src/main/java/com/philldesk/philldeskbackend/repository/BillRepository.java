package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.dto.BillProjection;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

       Optional<Bill> findByBillNumber(String billNumber);

       Optional<Bill> findByPrescription(Prescription prescription);

       List<Bill> findByCustomer(User customer);

       List<Bill> findByPharmacist(User pharmacist);

       @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.billItems WHERE b.paymentStatus = :status")
       List<Bill> findByPaymentStatus(@Param("status") Bill.PaymentStatus paymentStatus);

       @Query("SELECT b FROM Bill b WHERE b.paymentMethod = :method")
       List<Bill> findByPaymentMethod(@Param("method") Bill.PaymentMethod paymentMethod);

       @Query("SELECT b FROM Bill b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
       List<Bill> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId);

       @Query("SELECT b FROM Bill b JOIN FETCH b.customer LEFT JOIN FETCH b.pharmacist LEFT JOIN FETCH b.prescription LEFT JOIN FETCH b.shippingDetails WHERE b.id = :id")
       Optional<Bill> findByIdWithDetails(@Param("id") Long id);

       @Query("SELECT b FROM Bill b WHERE b.pharmacist.id = :pharmacistId ORDER BY b.createdAt DESC")
       List<Bill> findByPharmacistIdOrderByCreatedAtDesc(@Param("pharmacistId") Long pharmacistId);

       @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.shippingDetails WHERE b.pharmacist.id = :pharmacistId AND b.paymentType = 'ONLINE' AND b.paymentStatus = 'PAID' ORDER BY b.createdAt DESC")
       List<Bill> findOnlinePaidBillsByPharmacistWithShipping(@Param("pharmacistId") Long pharmacistId);

       @Query("SELECT b FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate")
       List<Bill> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT b FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.paymentStatus = 'PAID'")
       List<Bill> findPaidBillsBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.paymentStatus = 'PAID'")
       BigDecimal calculateTotalRevenueBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COUNT(b) FROM Bill b WHERE b.paymentStatus = :status")
       Long countByPaymentStatus(@Param("status") Bill.PaymentStatus status);

       @Query("SELECT b FROM Bill b WHERE b.totalAmount >= :minAmount AND b.totalAmount <= :maxAmount")
       List<Bill> findByTotalAmountBetween(@Param("minAmount") BigDecimal minAmount,
                     @Param("maxAmount") BigDecimal maxAmount);

       @Query("SELECT AVG(b.totalAmount) FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.paymentStatus = 'PAID'")
       Double calculateAverageOrderValue(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT b FROM Bill b WHERE " +
                     "(LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(b.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(b.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                     "ORDER BY b.createdAt DESC")
       List<Bill> searchBills(@Param("searchTerm") String searchTerm);

       @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate AND b.paymentStatus = 'PAID'")
       BigDecimal getTotalRevenueBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COUNT(b) FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate")
       Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Simple queries without deep joins to avoid circular references
       @Query("SELECT b FROM Bill b WHERE b.paymentStatus = :status ORDER BY b.createdAt DESC")
       List<Bill> findByPaymentStatusSimple(@Param("status") Bill.PaymentStatus status);

       // Projection queries to completely avoid entity loading
       @Query("SELECT b.id as id, b.billNumber as billNumber, b.subtotal as subtotal, " +
                     "b.discount as discount, b.tax as tax, b.totalAmount as totalAmount, " +
                     "b.paymentStatus as paymentStatus, b.paymentMethod as paymentMethod, " +
                     "b.paymentType as paymentType, b.notes as notes, " +
                     "b.createdAt as createdAt, b.updatedAt as updatedAt, b.paidAt as paidAt, " +
                     "c.id as customerId, c.firstName as customerFirstName, c.lastName as customerLastName, " +
                     "p.id as prescriptionId, p.prescriptionNumber as prescriptionNumber " +
                     "FROM Bill b LEFT JOIN b.customer c LEFT JOIN b.prescription p " +
                     "WHERE b.paymentStatus = :status ORDER BY b.createdAt DESC")
       List<BillProjection> findBillProjectionsByPaymentStatus(@Param("status") Bill.PaymentStatus status);
}
