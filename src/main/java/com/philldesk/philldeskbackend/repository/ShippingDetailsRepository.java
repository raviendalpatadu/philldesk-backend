package com.philldesk.philldeskbackend.repository;

import com.philldesk.philldeskbackend.entity.ShippingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ShippingDetails entity
 * Handles data access operations for shipping and delivery information
 */
@Repository
public interface ShippingDetailsRepository extends JpaRepository<ShippingDetails, Long> {
    
    /**
     * Find shipping details by bill ID
     */
    Optional<ShippingDetails> findByBillId(Long billId);
    
    /**
     * Find all shipping details by shipping status
     */
    List<ShippingDetails> findByShippingStatus(ShippingDetails.ShippingStatus shippingStatus);
    
    /**
     * Find all pending deliveries (PENDING, PROCESSING, SHIPPED)
     */
    @Query("SELECT s FROM ShippingDetails s WHERE s.shippingStatus IN ('PENDING', 'PROCESSING', 'SHIPPED')")
    List<ShippingDetails> findAllPendingDeliveries();
    
    /**
     * Find shipping details by tracking number
     */
    Optional<ShippingDetails> findByTrackingNumber(String trackingNumber);
    
    /**
     * Find all shipping details by city for delivery planning
     */
    List<ShippingDetails> findByCityAndShippingStatusIn(
        String city, 
        List<ShippingDetails.ShippingStatus> statuses
    );
    
    /**
     * Count deliveries by status
     */
    long countByShippingStatus(ShippingDetails.ShippingStatus shippingStatus);
    
    /**
     * Find shipping details by postal code for route optimization
     */
    List<ShippingDetails> findByPostalCodeAndShippingStatusIn(
        String postalCode,
        List<ShippingDetails.ShippingStatus> statuses
    );
}
