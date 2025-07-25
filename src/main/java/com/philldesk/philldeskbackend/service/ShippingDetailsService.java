package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.dto.ShippingDetailsDTO;
import com.philldesk.philldeskbackend.entity.ShippingDetails;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing shipping and delivery operations
 */
public interface ShippingDetailsService {
    
    /**
     * Create shipping details for a bill
     */
    ShippingDetails createShippingDetails(Long billId, ShippingDetailsDTO shippingDetailsDTO);
    
    /**
     * Get shipping details by bill ID
     */
    Optional<ShippingDetails> getShippingDetailsByBillId(Long billId);
    
    /**
     * Get shipping details by ID
     */
    Optional<ShippingDetails> getShippingDetailsById(Long id);
    
    /**
     * Update shipping status
     */
    ShippingDetails updateShippingStatus(Long id, ShippingDetails.ShippingStatus status);
    
    /**
     * Update tracking information
     */
    ShippingDetails updateTrackingInfo(Long id, String trackingNumber, String trackingUrl);
    
    /**
     * Get all shipping details by status
     */
    List<ShippingDetails> getShippingDetailsByStatus(ShippingDetails.ShippingStatus status);
    
    /**
     * Get all pending deliveries for pharmacist view
     */
    List<ShippingDetails> getAllPendingDeliveries();
    
    /**
     * Get shipping details by tracking number
     */
    Optional<ShippingDetails> getShippingDetailsByTrackingNumber(String trackingNumber);
    
    /**
     * Get deliveries by city and status for route planning
     */
    List<ShippingDetails> getDeliveriesByCityAndStatus(String city, List<ShippingDetails.ShippingStatus> statuses);
    
    /**
     * Mark delivery as completed
     */
    ShippingDetails markDeliveryCompleted(Long id, String deliveryNotes);
    
    /**
     * Cancel delivery
     */
    ShippingDetails cancelDelivery(Long id, String cancellationReason);
    
    /**
     * Generate tracking number
     */
    String generateTrackingNumber();
    
    /**
     * Update shipping details
     */
    ShippingDetails updateShippingDetails(Long id, ShippingDetailsDTO shippingDetailsDTO);
}
