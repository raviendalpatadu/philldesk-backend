package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Bill;
import com.philldesk.philldeskbackend.entity.Prescription;
import com.philldesk.philldeskbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillService {
    List<Bill> getAllBills();
    Page<Bill> getAllBills(Pageable pageable);
    Optional<Bill> getBillById(Long id);
    Optional<Bill> getBillByPrescription(Prescription prescription);
    List<Bill> getBillsByCustomer(User customer);
    List<Bill> getBillsByCustomerId(Long customerId);
    List<Bill> getBillsByStatus(Bill.PaymentStatus status);
    List<Bill> getBillsByPaymentMethod(Bill.PaymentMethod paymentMethod);
    List<Bill> getBillsByDateRange(LocalDate startDate, LocalDate endDate);
    List<Bill> getPendingBills();
    List<Bill> getPaidBills();
    Bill generateBillFromPrescription(Prescription prescription);
    Bill saveBill(Bill bill);
    Bill updateBill(Bill bill);
    void deleteBill(Long id);
    void updateStatus(Long billId, Bill.PaymentStatus status);
    void markAsPaid(Long billId, Bill.PaymentMethod paymentMethod);
    BigDecimal calculateTotalAmount(Prescription prescription);
    List<Bill> searchBills(String searchTerm);
    BigDecimal getTotalRevenue(LocalDate startDate, LocalDate endDate);
    Long getTotalBillCount(LocalDate startDate, LocalDate endDate);
}
