package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.service.PdfGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Reports Controller
 * 
 * This controller provides endpoints for generating and downloading reports,
 * including PDF exports of sales, inventory, and user activity reports.
 * All endpoints require ADMIN role authorization.
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
    private static final DateTimeFormatter FILENAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String ATTACHMENT_HEADER = "attachment";

    private final PdfGenerationService pdfGenerationService;

    @Autowired
    public ReportsController(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * Generate and download PDF report
     * 
     * @param reportData The report data to include in the PDF
     * @return PDF file as byte array with appropriate headers
     */
    @PostMapping("/download/pdf")
    public ResponseEntity<byte[]> downloadPdfReport(@RequestBody Map<String, Object> reportData) {
        try {
            logger.info("Generating PDF report...");
            
            // Extract report parameters
            String reportTitle = (String) reportData.getOrDefault("title", "PhillDesk Reports");
            String startDate = (String) reportData.getOrDefault("startDate", "");
            String endDate = (String) reportData.getOrDefault("endDate", "");
            String dateRange = startDate + " to " + endDate;
            
            // Generate PDF
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportTitle, reportData, dateRange);
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(FILENAME_DATE_FORMAT);
            String filename = "PhillDesk_Report_" + timestamp + ".pdf";
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(ATTACHMENT_HEADER, filename);
            headers.setContentLength(pdfBytes.length);
            
            logger.info("PDF report generated successfully: {}", filename);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate and download sales report PDF
     * 
     * @param startDate Start date for the report
     * @param endDate End date for the report
     * @param salesData Sales data to include
     * @return PDF file as byte array
     */
    @PostMapping("/sales/pdf")
    public ResponseEntity<byte[]> downloadSalesReportPdf(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestBody Map<String, Object> salesData) {
        try {
            logger.info("Generating sales report PDF for period: {} to {}", startDate, endDate);
            
            String reportTitle = "Sales Report";
            String dateRange = startDate + " to " + endDate;
            
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportTitle, salesData, dateRange);
            
            String timestamp = LocalDateTime.now().format(FILENAME_DATE_FORMAT);
            String filename = "Sales_Report_" + timestamp + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(ATTACHMENT_HEADER, filename);
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Error generating sales report PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate and download inventory report PDF
     * 
     * @param inventoryData Inventory data to include
     * @return PDF file as byte array
     */
    @PostMapping("/inventory/pdf")
    public ResponseEntity<byte[]> downloadInventoryReportPdf(@RequestBody Map<String, Object> inventoryData) {
        try {
            logger.info("Generating inventory report PDF...");
            
            String reportTitle = "Inventory Report";
            String dateRange = "As of " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportTitle, inventoryData, dateRange);
            
            String timestamp = LocalDateTime.now().format(FILENAME_DATE_FORMAT);
            String filename = "Inventory_Report_" + timestamp + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(ATTACHMENT_HEADER, filename);
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Error generating inventory report PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate and download user activity report PDF
     * 
     * @param userActivityData User activity data to include
     * @return PDF file as byte array
     */
    @PostMapping("/user-activity/pdf")
    public ResponseEntity<byte[]> downloadUserActivityReportPdf(@RequestBody Map<String, Object> userActivityData) {
        try {
            logger.info("Generating user activity report PDF...");
            
            String reportTitle = "User Activity Report";
            String dateRange = "As of " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportTitle, userActivityData, dateRange);
            
            String timestamp = LocalDateTime.now().format(FILENAME_DATE_FORMAT);
            String filename = "User_Activity_Report_" + timestamp + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(ATTACHMENT_HEADER, filename);
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Error generating user activity report PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
