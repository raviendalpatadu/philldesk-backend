package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.dto.ShippingDetailsDTO;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.ShippingDetails;
import com.philldesk.philldeskbackend.repository.BillRepository;
import com.philldesk.philldeskbackend.repository.ShippingDetailsRepository;
import com.philldesk.philldeskbackend.service.ShippingDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Implementation of ShippingDetailsService
 * Handles shipping and delivery operations
 */
@Service
@Transactional
public class ShippingDetailsServiceImpl implements ShippingDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShippingDetailsServiceImpl.class);
    
    @Autowired
    private ShippingDetailsRepository shippingDetailsRepository;
    
    @Autowired
    private BillRepository billRepository;
    
    @Override
    public ShippingDetails createShippingDetails(Long billId, ShippingDetailsDTO shippingDetailsDTO) {
        logger.info("Creating shipping details for bill ID: {}", billId);
        
        // Verify bill exists
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new RuntimeException("Bill not found with ID: " + billId));
        
        // Check if shipping details already exist for this bill
        Optional<ShippingDetails> existingShipping = shippingDetailsRepository.findByBillId(billId);
        if (existingShipping.isPresent()) {
            throw new RuntimeException("Shipping details already exist for bill ID: " + billId);
        }
        
        // Create new shipping details
        ShippingDetails shippingDetails = new ShippingDetails();
        shippingDetails.setBill(bill);
        shippingDetails.setRecipientName(shippingDetailsDTO.getRecipientName());
        shippingDetails.setContactPhone(shippingDetailsDTO.getContactPhone());
        shippingDetails.setAlternatePhone(shippingDetailsDTO.getAlternatePhone());
        shippingDetails.setEmail(shippingDetailsDTO.getEmail());
        shippingDetails.setAddressLine1(shippingDetailsDTO.getAddressLine1());
        shippingDetails.setAddressLine2(shippingDetailsDTO.getAddressLine2());
        shippingDetails.setCity(shippingDetailsDTO.getCity());
        shippingDetails.setStateProvince(shippingDetailsDTO.getStateProvince());
        shippingDetails.setPostalCode(shippingDetailsDTO.getPostalCode());
        shippingDetails.setCountry(shippingDetailsDTO.getCountry());
        shippingDetails.setDeliveryInstructions(shippingDetailsDTO.getDeliveryInstructions());
        shippingDetails.setPreferredDeliveryTime(shippingDetailsDTO.getPreferredDeliveryTime());
        shippingDetails.setShippingStatus(ShippingDetails.ShippingStatus.PENDING);
        shippingDetails.setTrackingNumber(generateTrackingNumber());
        shippingDetails.setOrderedAt(LocalDateTime.now());
        
        ShippingDetails savedShipping = shippingDetailsRepository.save(shippingDetails);
        logger.info("Created shipping details with ID: {} for bill: {}", savedShipping.getId(), billId);
        
        return savedShipping;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ShippingDetails> getShippingDetailsByBillId(Long billId) {
        return shippingDetailsRepository.findByBillId(billId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ShippingDetails> getShippingDetailsById(Long id) {
        return shippingDetailsRepository.findById(id);
    }
    
    @Override
    public ShippingDetails updateShippingStatus(Long id, ShippingDetails.ShippingStatus status) {
        logger.info("Updating shipping status for ID: {} to {}", id, status);
        
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping details not found with ID: " + id));
        
        ShippingDetails.ShippingStatus oldStatus = shippingDetails.getShippingStatus();
        shippingDetails.setShippingStatus(status);
        
        // Update timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case PROCESSING:
                if (shippingDetails.getProcessedAt() == null) {
                    shippingDetails.setProcessedAt(now);
                }
                break;
            case SHIPPED:
                if (shippingDetails.getShippedAt() == null) {
                    shippingDetails.setShippedAt(now);
                }
                break;
            case DELIVERED:
                if (shippingDetails.getDeliveredAt() == null) {
                    shippingDetails.setDeliveredAt(now);
                }
                break;
            case CANCELLED:
                if (shippingDetails.getCancelledAt() == null) {
                    shippingDetails.setCancelledAt(now);
                }
                break;
        }
        
        ShippingDetails updated = shippingDetailsRepository.save(shippingDetails);
        logger.info("Updated shipping status from {} to {} for ID: {}", oldStatus, status, id);
        
        return updated;
    }
    
    @Override
    public ShippingDetails updateTrackingInfo(Long id, String trackingNumber, String trackingUrl) {
        logger.info("Updating tracking info for shipping ID: {}", id);
        
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping details not found with ID: " + id));
        
        shippingDetails.setTrackingNumber(trackingNumber);
        shippingDetails.setTrackingUrl(trackingUrl);
        
        return shippingDetailsRepository.save(shippingDetails);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShippingDetails> getShippingDetailsByStatus(ShippingDetails.ShippingStatus status) {
        return shippingDetailsRepository.findByShippingStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShippingDetails> getAllPendingDeliveries() {
        return shippingDetailsRepository.findAllPendingDeliveries();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ShippingDetails> getShippingDetailsByTrackingNumber(String trackingNumber) {
        return shippingDetailsRepository.findByTrackingNumber(trackingNumber);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShippingDetails> getDeliveriesByCityAndStatus(String city, List<ShippingDetails.ShippingStatus> statuses) {
        return shippingDetailsRepository.findByCityAndShippingStatusIn(city, statuses);
    }
    
    @Override
    public ShippingDetails markDeliveryCompleted(Long id, String deliveryNotes) {
        logger.info("Marking delivery as completed for shipping ID: {}", id);
        
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping details not found with ID: " + id));
        
        shippingDetails.setShippingStatus(ShippingDetails.ShippingStatus.DELIVERED);
        shippingDetails.setDeliveredAt(LocalDateTime.now());
        shippingDetails.setDeliveryNotes(deliveryNotes);
        
        return shippingDetailsRepository.save(shippingDetails);
    }
    
    @Override
    public ShippingDetails cancelDelivery(Long id, String cancellationReason) {
        logger.info("Cancelling delivery for shipping ID: {}", id);
        
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping details not found with ID: " + id));
        
        shippingDetails.setShippingStatus(ShippingDetails.ShippingStatus.CANCELLED);
        shippingDetails.setCancelledAt(LocalDateTime.now());
        shippingDetails.setCancellationReason(cancellationReason);
        
        return shippingDetailsRepository.save(shippingDetails);
    }
    
    @Override
    public String generateTrackingNumber() {
        // Generate a tracking number in format: PD-YYYYMMDD-XXXXXX
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        int randomNumber = random.nextInt(999999);
        
        return String.format("PD-%s-%06d", datePrefix, randomNumber);
    }
    
    @Override
    public ShippingDetails updateShippingDetails(Long id, ShippingDetailsDTO shippingDetailsDTO) {
        logger.info("Updating shipping details for ID: {}", id);
        
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping details not found with ID: " + id));
        
        // Update mutable fields
        shippingDetails.setRecipientName(shippingDetailsDTO.getRecipientName());
        shippingDetails.setContactPhone(shippingDetailsDTO.getContactPhone());
        shippingDetails.setAlternatePhone(shippingDetailsDTO.getAlternatePhone());
        shippingDetails.setEmail(shippingDetailsDTO.getEmail());
        shippingDetails.setAddressLine1(shippingDetailsDTO.getAddressLine1());
        shippingDetails.setAddressLine2(shippingDetailsDTO.getAddressLine2());
        shippingDetails.setCity(shippingDetailsDTO.getCity());
        shippingDetails.setStateProvince(shippingDetailsDTO.getStateProvince());
        shippingDetails.setPostalCode(shippingDetailsDTO.getPostalCode());
        shippingDetails.setCountry(shippingDetailsDTO.getCountry());
        shippingDetails.setDeliveryInstructions(shippingDetailsDTO.getDeliveryInstructions());
        shippingDetails.setPreferredDeliveryTime(shippingDetailsDTO.getPreferredDeliveryTime());
        
        return shippingDetailsRepository.save(shippingDetails);
    }
}
