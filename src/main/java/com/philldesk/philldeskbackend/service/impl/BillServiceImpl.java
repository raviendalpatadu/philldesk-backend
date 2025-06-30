package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.entity.*;
import com.philldesk.philldeskbackend.repository.BillRepository;
import com.philldesk.philldeskbackend.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;

    @Autowired
    public BillServiceImpl(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Bill> getAllBills(Pageable pageable) {
        return billRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bill> getBillByPrescription(Prescription prescription) {
        return billRepository.findByPrescription(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByCustomer(User customer) {
        return billRepository.findByCustomer(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByCustomerId(Long customerId) {
        return billRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByStatus(Bill.PaymentStatus status) {
        return billRepository.findByPaymentStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByPaymentMethod(Bill.PaymentMethod paymentMethod) {
        return billRepository.findByPaymentMethod(paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getPendingBills() {
        return billRepository.findByPaymentStatus(Bill.PaymentStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> getPaidBills() {
        return billRepository.findByPaymentStatus(Bill.PaymentStatus.PAID);
    }

    @Override
    public Bill generateBillFromPrescription(Prescription prescription) {
        Bill bill = new Bill();
        bill.setBillNumber(generateBillNumber());
        bill.setPrescription(prescription);
        bill.setCustomer(prescription.getCustomer());
        bill.setPharmacist(prescription.getPharmacist());
        bill.setPaymentStatus(Bill.PaymentStatus.PENDING);
        
        // Create bill items from prescription items
        Set<BillItem> billItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (PrescriptionItem prescriptionItem : prescription.getPrescriptionItems()) {
            BillItem billItem = new BillItem();
            billItem.setBill(bill);
            billItem.setMedicine(prescriptionItem.getMedicine());
            billItem.setQuantity(prescriptionItem.getQuantity());
            billItem.setUnitPrice(prescriptionItem.getMedicine().getUnitPrice());
            billItem.setTotalPrice(prescriptionItem.getMedicine().getUnitPrice()
                    .multiply(BigDecimal.valueOf(prescriptionItem.getQuantity())));
            
            billItems.add(billItem);
            subtotal = subtotal.add(billItem.getTotalPrice());
        }
        
        bill.setBillItems(billItems);
        bill.setSubtotal(subtotal);
        bill.setDiscount(BigDecimal.ZERO);
        bill.setTax(BigDecimal.ZERO);
        
        return saveBill(bill);
    }

    @Override
    public Bill saveBill(Bill bill) {
        if (bill.getBillNumber() == null || bill.getBillNumber().isEmpty()) {
            bill.setBillNumber(generateBillNumber());
        }
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    @Override
    public Bill updateBill(Bill bill) {
        Optional<Bill> existingBill = billRepository.findById(bill.getId());
        if (existingBill.isPresent()) {
            bill.setCreatedAt(existingBill.get().getCreatedAt());
        }
        bill.setUpdatedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    @Override
    public void deleteBill(Long id) {
        billRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long billId, Bill.PaymentStatus status) {
        Optional<Bill> bill = billRepository.findById(billId);
        if (bill.isPresent()) {
            Bill existingBill = bill.get();
            existingBill.setPaymentStatus(status);
            existingBill.setUpdatedAt(LocalDateTime.now());
            if (status == Bill.PaymentStatus.PAID) {
                existingBill.setPaidAt(LocalDateTime.now());
            }
            billRepository.save(existingBill);
        }
    }

    @Override
    public void markAsPaid(Long billId, Bill.PaymentMethod paymentMethod) {
        Optional<Bill> bill = billRepository.findById(billId);
        if (bill.isPresent()) {
            Bill existingBill = bill.get();
            existingBill.setPaymentStatus(Bill.PaymentStatus.PAID);
            existingBill.setPaymentMethod(paymentMethod);
            existingBill.setPaidAt(LocalDateTime.now());
            existingBill.setUpdatedAt(LocalDateTime.now());
            billRepository.save(existingBill);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(Prescription prescription) {
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionItem item : prescription.getPrescriptionItems()) {
            BigDecimal itemTotal = item.getMedicine().getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bill> searchBills(String searchTerm) {
        return billRepository.searchBills(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.getTotalRevenueBetween(startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalBillCount(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return billRepository.countByCreatedAtBetween(startDateTime, endDateTime);
    }

    private String generateBillNumber() {
        // Generate unique bill number with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "BILL-" + timestamp;
    }
}
