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
    
    /**
     * Generate PDF report with custom data
     * @param reportTitle The title of the report
     * @param reportData The data to include in the report
     * @param dateRange The date range for the report
     * @return PDF content as byte array
     * @throws Exception if PDF generation fails
     */
    byte[] generateReportPdf(String reportTitle, Object reportData, String dateRange) throws Exception;
}
