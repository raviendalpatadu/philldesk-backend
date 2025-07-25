package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Bill;

/**
 * Interface for PDF generation services
 */
public interface PdfGenerationService {
    
    /**
     * Generate PDF for a bill
     * @param bill The bill to generate PDF for
     * @return PDF content as byte array
     * @throws Exception if PDF generation fails
     */
    byte[] generateBillPdf(Bill bill) throws Exception;
    
    /**
     * Generate PDF receipt for a bill
     * @param bill The bill to generate receipt for
     * @return PDF content as byte array
     * @throws Exception if PDF generation fails
     */
    byte[] generateReceiptPdf(Bill bill) throws Exception;
}
