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
}