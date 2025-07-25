package com.philldesk.philldeskbackend.dto;

import com.philldesk.philldeskbackend.entity.ShippingDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for ShippingDetails
 * Used for API communication and data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingDetailsDTO {
    
    private Long id;
    private String recipientName;
    private String contactPhone;
    private String alternatePhone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String deliveryInstructions;
    private String preferredDeliveryTime;
    private String shippingStatus;
    private String trackingNumber;
    private String trackingUrl;
    private LocalDateTime orderedAt;
    private LocalDateTime processedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String deliveryNotes;
    private String cancellationReason;
    
    /**
     * Convert ShippingDetails entity to DTO
     */
    public static ShippingDetailsDTO fromEntity(ShippingDetails shippingDetails) {
        if (shippingDetails == null) {
            return null;
        }
        
        ShippingDetailsDTO dto = new ShippingDetailsDTO();
        dto.setId(shippingDetails.getId());
        dto.setRecipientName(shippingDetails.getRecipientName());
        dto.setContactPhone(shippingDetails.getContactPhone());
        dto.setAlternatePhone(shippingDetails.getAlternatePhone());
        dto.setEmail(shippingDetails.getEmail());
        dto.setAddressLine1(shippingDetails.getAddressLine1());
        dto.setAddressLine2(shippingDetails.getAddressLine2());
        dto.setCity(shippingDetails.getCity());
        dto.setStateProvince(shippingDetails.getStateProvince());
        dto.setPostalCode(shippingDetails.getPostalCode());
        dto.setCountry(shippingDetails.getCountry());
        dto.setDeliveryInstructions(shippingDetails.getDeliveryInstructions());
        dto.setPreferredDeliveryTime(shippingDetails.getPreferredDeliveryTime());
        dto.setShippingStatus(shippingDetails.getShippingStatus().toString());
        dto.setTrackingNumber(shippingDetails.getTrackingNumber());
        dto.setTrackingUrl(shippingDetails.getTrackingUrl());
        dto.setOrderedAt(shippingDetails.getOrderedAt());
        dto.setProcessedAt(shippingDetails.getProcessedAt());
        dto.setShippedAt(shippingDetails.getShippedAt());
        dto.setDeliveredAt(shippingDetails.getDeliveredAt());
        dto.setCancelledAt(shippingDetails.getCancelledAt());
        dto.setDeliveryNotes(shippingDetails.getDeliveryNotes());
        dto.setCancellationReason(shippingDetails.getCancellationReason());
        
        return dto;
    }
}