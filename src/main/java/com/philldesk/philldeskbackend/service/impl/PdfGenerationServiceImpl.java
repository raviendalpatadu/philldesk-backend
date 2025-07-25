package com.philldesk.philldeskbackend.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.BillItem;
import com.philldesk.philldeskbackend.service.PdfGenerationService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfGenerationServiceImpl implements PdfGenerationService {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override
    public byte[] generateBillPdf(Bill bill) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add header
        addHeader(document, bill);
        
        // Add customer information
        addCustomerInfo(document, bill);
        
        // Add bill items table
        addBillItemsTable(document, bill);
        
        // Add totals
        addTotals(document, bill);
        
        // Add footer
        addFooter(document, bill);

        document.close();
        return baos.toByteArray();
    }

    @Override
    public byte[] generateReceiptPdf(Bill bill) throws Exception {
        // For now, use the same format as bill PDF
        return generateBillPdf(bill);
    }

    private void addHeader(Document document, Bill bill) {
        // Company header
        Paragraph header = new Paragraph("PhillDesk Pharmacy")
                .setFontSize(24)
                .setBold();
        document.add(header);

        Paragraph subHeader = new Paragraph("Digital Pharmacy Services")
                .setFontSize(12)
                .setMarginBottom(20);
        document.add(subHeader);

        // Bill information
        Paragraph billInfo = new Paragraph()
                .add("Bill Number: " + bill.getBillNumber()).add("\n")
                .add("Date: " + bill.getCreatedAt().format(DATE_FORMAT)).add("\n")
                .add("Status: " + bill.getPaymentStatus().toString())
                .setFontSize(10)
                .setMarginBottom(20);
        document.add(billInfo);
    }

    private void addCustomerInfo(Document document, Bill bill) {
        // Customer information section
        Paragraph customerTitle = new Paragraph("BILL TO:")
                .setFontSize(12)
                .setBold()
                .setMarginBottom(5);
        document.add(customerTitle);

        String customerName = bill.getCustomer().getFirstName() + " " + bill.getCustomer().getLastName();
        Paragraph customerInfo = new Paragraph()
                .add("Customer: " + customerName).add("\n")
                .add("Email: " + bill.getCustomer().getEmail()).add("\n")
                .setFontSize(10)
                .setMarginBottom(20);
        document.add(customerInfo);

        // Add prescription information if available
        if (bill.getPrescription() != null) {
            Paragraph prescriptionInfo = new Paragraph()
                    .add("Prescription Number: " + bill.getPrescription().getPrescriptionNumber()).add("\n")
                    .add("Doctor: " + bill.getPrescription().getDoctorName()).add("\n")
                    .setFontSize(10)
                    .setMarginBottom(20);
            document.add(prescriptionInfo);
        }
    }

    private void addBillItemsTable(Document document, Bill bill) {
        // Items table
        Paragraph itemsTitle = new Paragraph("ITEMS:")
                .setFontSize(12)
                .setBold()
                .setMarginBottom(10);
        document.add(itemsTitle);

        // Create a simple table with 4 columns
        Table table = new Table(4);

        // Table headers
        table.addHeaderCell(new Cell().add(new Paragraph("Item").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Price").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Total").setBold()));

        // Add bill items
        if (bill.getBillItems() != null && !bill.getBillItems().isEmpty()) {
            for (BillItem item : bill.getBillItems()) {
                table.addCell(new Cell().add(new Paragraph(item.getMedicine().getName() + " (" + item.getMedicine().getStrength() + " " + item.getMedicine().getDosageForm() + ")")));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))));
                table.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(item.getUnitPrice()))));
                table.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(item.getTotalPrice()))));
            }
        } else {
            // Add a single row for prescription items
            table.addCell(new Cell().add(new Paragraph("Prescription Items")));
            table.addCell(new Cell().add(new Paragraph("1")));
            table.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(bill.getTotalAmount()))));
            table.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(bill.getTotalAmount()))));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTotals(Document document, Bill bill) {
        // Totals section
        Paragraph totalsTitle = new Paragraph("TOTALS:")
                .setFontSize(12)
                .setBold()
                .setMarginBottom(10);
        document.add(totalsTitle);

        // Create totals table
        Table totalsTable = new Table(2);

        // Add total rows
        totalsTable.addCell(new Cell().add(new Paragraph("Subtotal:")));
        totalsTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(bill.getTotalAmount()))));

        // Add delivery fee if applicable
        if (bill.getShippingDetails() != null && bill.getShippingDetails().getDeliveryFee() != null) {
            totalsTable.addCell(new Cell().add(new Paragraph("Delivery Fee:")));
            totalsTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(bill.getShippingDetails().getDeliveryFee()))));
        }

        // Grand total
        BigDecimal grandTotal = bill.getTotalAmount();
        if (bill.getShippingDetails() != null && bill.getShippingDetails().getDeliveryFee() != null) {
            grandTotal = grandTotal.add(bill.getShippingDetails().getDeliveryFee());
        }

        totalsTable.addCell(new Cell().add(new Paragraph("TOTAL:").setBold()));
        totalsTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(grandTotal)).setBold()));

        document.add(totalsTable);
        document.add(new Paragraph("\n"));
    }

    private void addFooter(Document document, Bill bill) {
        // Payment information
        if (bill.getPaymentMethod() != null) {
            Paragraph paymentInfo = new Paragraph()
                    .add("Payment Method: " + bill.getPaymentMethod().toString()).add("\n")
                    .add("Payment Status: " + bill.getPaymentStatus().toString()).add("\n")
                    .setFontSize(10)
                    .setMarginBottom(20);
            document.add(paymentInfo);
        }

        // Footer
        Paragraph footer = new Paragraph()
                .add("Thank you for choosing PhillDesk Pharmacy!").add("\n")
                .add("For support, contact us at support@philldesk.com")
                .setFontSize(10);
        document.add(footer);
    }

    @Override
    public byte[] generateReportPdf(String reportTitle, Object reportData, String dateRange) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add header
        addReportHeader(document, reportTitle, dateRange);
        
        // Add report content based on data type
        addReportContent(document, reportData);
        
        // Add footer
        addReportFooter(document);

        document.close();
        return baos.toByteArray();
    }

    private void addReportHeader(Document document, String title, String dateRange) {
        // Company header
        Paragraph header = new Paragraph()
                .add("PhillDesk Pharmacy Management System").add("\n")
                .add("Administrative Reports")
                .setFontSize(16)
                .setBold()
                .setMarginBottom(20);
        document.add(header);

        // Report title
        Paragraph reportTitle = new Paragraph(title)
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(reportTitle);

        // Date range
        Paragraph dateInfo = new Paragraph()
                .add("Report Period: " + dateRange).add("\n")
                .add("Generated on: " + DATETIME_FORMAT.format(java.time.LocalDateTime.now()))
                .setFontSize(10)
                .setMarginBottom(20);
        document.add(dateInfo);
    }

    private void addReportContent(Document document, Object reportData) {
        if (reportData instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) reportData;
            
            // Summary statistics
            if (data.containsKey("summaryStats")) {
                addSummaryStats(document, (java.util.Map<String, Object>) data.get("summaryStats"));
            }
            
            // Sales data
            if (data.containsKey("salesData")) {
                addSalesDataTable(document, (java.util.List<?>) data.get("salesData"));
            }
            
            // Inventory data
            if (data.containsKey("inventoryData")) {
                addInventoryDataTable(document, (java.util.List<?>) data.get("inventoryData"));
            }
            
            // User activity data
            if (data.containsKey("userActivityData")) {
                addUserActivityTable(document, (java.util.List<?>) data.get("userActivityData"));
            }
        }
    }

    private void addSummaryStats(Document document, java.util.Map<String, Object> summaryStats) {
        Paragraph summaryTitle = new Paragraph("Summary Statistics")
                .setFontSize(12)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(summaryTitle);

        Table summaryTable = new Table(2);
        summaryTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Add summary statistics
        if (summaryStats.containsKey("totalRevenue")) {
            summaryTable.addCell(new Cell().add(new Paragraph("Total Revenue")));
            summaryTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(summaryStats.get("totalRevenue")))));
        }
        if (summaryStats.containsKey("totalInvoices")) {
            summaryTable.addCell(new Cell().add(new Paragraph("Total Orders")));
            summaryTable.addCell(new Cell().add(new Paragraph(summaryStats.get("totalInvoices").toString())));
        }
        if (summaryStats.containsKey("averageOrderValue")) {
            summaryTable.addCell(new Cell().add(new Paragraph("Average Order Value")));
            summaryTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(summaryStats.get("averageOrderValue")))));
        }
        if (summaryStats.containsKey("activeUsers")) {
            summaryTable.addCell(new Cell().add(new Paragraph("Active Users")));
            summaryTable.addCell(new Cell().add(new Paragraph(summaryStats.get("activeUsers").toString())));
        }

        document.add(summaryTable);
    }

    private void addSalesDataTable(Document document, java.util.List<?> salesData) {
        if (salesData == null || salesData.isEmpty()) return;

        Paragraph salesTitle = new Paragraph("Sales Report")
                .setFontSize(12)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(salesTitle);

        Table salesTable = new Table(6);
        salesTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Headers
        salesTable.addHeaderCell(new Cell().add(new Paragraph("Date")).setBold());
        salesTable.addHeaderCell(new Cell().add(new Paragraph("Invoices")).setBold());
        salesTable.addHeaderCell(new Cell().add(new Paragraph("Revenue")).setBold());
        salesTable.addHeaderCell(new Cell().add(new Paragraph("Prescriptions")).setBold());
        salesTable.addHeaderCell(new Cell().add(new Paragraph("OTC Sales")).setBold());
        salesTable.addHeaderCell(new Cell().add(new Paragraph("Avg Order Value")).setBold());

        // Data rows (simplified - you would need to properly cast and extract data)
        for (Object row : salesData) {
            if (row instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) row;
                salesTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("date", "").toString())));
                salesTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("invoices", "0").toString())));
                salesTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(data.getOrDefault("revenue", 0)))));
                salesTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("prescriptions", "0").toString())));
                salesTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("otc", "0").toString())));
                salesTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(data.getOrDefault("averageValue", 0)))));
            }
        }

        document.add(salesTable);
    }

    private void addInventoryDataTable(Document document, java.util.List<?> inventoryData) {
        if (inventoryData == null || inventoryData.isEmpty()) return;

        Paragraph inventoryTitle = new Paragraph("Inventory Report")
                .setFontSize(12)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(inventoryTitle);

        Table inventoryTable = new Table(6);
        inventoryTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Headers
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Category")).setBold());
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Total Items")).setBold());
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Total Value")).setBold());
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Low Stock")).setBold());
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Expiring Soon")).setBold());
        inventoryTable.addHeaderCell(new Cell().add(new Paragraph("Turnover Rate")).setBold());

        // Data rows
        for (Object row : inventoryData) {
            if (row instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) row;
                inventoryTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("category", "").toString())));
                inventoryTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("totalItems", "0").toString())));
                inventoryTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMAT.format(data.getOrDefault("totalValue", 0)))));
                inventoryTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("lowStock", "0").toString())));
                inventoryTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("expiringSoon", "0").toString())));
                inventoryTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("turnoverRate", "0").toString() + "%")));
            }
        }

        document.add(inventoryTable);
    }

    private void addUserActivityTable(Document document, java.util.List<?> userActivityData) {
        if (userActivityData == null || userActivityData.isEmpty()) return;

        Paragraph activityTitle = new Paragraph("User Activity Report")
                .setFontSize(12)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(activityTitle);

        Table activityTable = new Table(5);
        activityTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Headers
        activityTable.addHeaderCell(new Cell().add(new Paragraph("Role")).setBold());
        activityTable.addHeaderCell(new Cell().add(new Paragraph("Active Users")).setBold());
        activityTable.addHeaderCell(new Cell().add(new Paragraph("Avg Session Time")).setBold());
        activityTable.addHeaderCell(new Cell().add(new Paragraph("Prescriptions")).setBold());
        activityTable.addHeaderCell(new Cell().add(new Paragraph("Efficiency")).setBold());

        // Data rows
        for (Object row : userActivityData) {
            if (row instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) row;
                activityTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("role", "").toString())));
                activityTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("activeUsers", "0") + " / " + data.getOrDefault("totalUsers", "0"))));
                activityTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("avgSessionTime", "").toString())));
                activityTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("prescriptionsProcessed", "0").toString())));
                activityTable.addCell(new Cell().add(new Paragraph(data.getOrDefault("efficiency", "0").toString() + "%")));
            }
        }

        document.add(activityTable);
    }

    private void addReportFooter(Document document) {
        Paragraph footer = new Paragraph()
                .add("This report was generated by PhillDesk Pharmacy Management System").add("\n")
                .add("For questions about this report, contact the system administrator")
                .setFontSize(8)
                .setMarginTop(30);
        document.add(footer);
    }
}