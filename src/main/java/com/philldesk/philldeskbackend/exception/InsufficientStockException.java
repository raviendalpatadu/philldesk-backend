package com.philldesk.philldeskbackend.exception;

public class InsufficientStockException extends RuntimeException {
    private final Long medicineId;
    private final Integer availableStock;
    private final Integer requestedQuantity;

    public InsufficientStockException(String message) {
        super(message);
        this.medicineId = null;
        this.availableStock = null;
        this.requestedQuantity = null;
    }

    public InsufficientStockException(Long medicineId, Integer availableStock, Integer requestedQuantity) {
        super(String.format("Insufficient stock for medicine ID %d. Available: %d, Requested: %d", 
              medicineId, availableStock, requestedQuantity));
        this.medicineId = medicineId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
}
