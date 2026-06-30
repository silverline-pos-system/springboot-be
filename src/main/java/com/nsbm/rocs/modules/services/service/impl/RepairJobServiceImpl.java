package com.nsbm.rocs.modules.services.service.impl;

import com.nsbm.rocs.entity.pos.Customer;
import com.nsbm.rocs.entity.services.RepairJob;
import com.nsbm.rocs.entity.services.RepairPayment;
import com.nsbm.rocs.entity.services.RepairStatusHistory;
import com.nsbm.rocs.entity.enums.RepairStatus;
import com.nsbm.rocs.modules.pos.repository.CustomerRepository;
import com.nsbm.rocs.modules.services.dto.RepairJobRequestDTO;
import com.nsbm.rocs.modules.services.repository.RepairJobRepository;
import com.nsbm.rocs.modules.services.repository.RepairStatusHistoryRepository;
import com.nsbm.rocs.modules.services.repository.RepairPaymentRepository;
import com.nsbm.rocs.modules.services.service.RepairJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairJobServiceImpl implements RepairJobService {

    private final RepairJobRepository repairJobRepository;
    private final RepairStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final RepairPaymentRepository paymentRepository;

    @Override
    @Transactional
    public RepairJob logRepairJob(RepairJobRequestDTO requestDTO) {
        // Find or create customer
        Customer customer = customerRepository.findByPhone(requestDTO.getContactNo())
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setName(requestDTO.getCustomerName());
                    newCustomer.setPhone(requestDTO.getContactNo());
                    return customerRepository.save(newCustomer);
                });

        RepairJob job = new RepairJob();
        job.setRepairNo("REP-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000));
        job.setBranchId(requestDTO.getBranchId() != null ? requestDTO.getBranchId() : 1L);
        job.setCustomerId(customer.getCustomerId());
        job.setDeviceBrand(requestDTO.getDeviceBrand());
        job.setDeviceModel(requestDTO.getDeviceModel());
        job.setImeiNo(requestDTO.getImeiNo());
        job.setProblemDescription(requestDTO.getProblemDescription());
        job.setStatus(RepairStatus.RECEIVED);
        job.setCreatedBy(requestDTO.getCreatedBy());

        RepairJob savedJob = repairJobRepository.save(job);

        // Save advance payment if provided
        if (requestDTO.getAdvancePayment() != null && requestDTO.getAdvancePayment().compareTo(BigDecimal.ZERO) > 0) {
            RepairPayment payment = new RepairPayment();
            payment.setRepairId(savedJob.getRepairId());
            payment.setAmount(requestDTO.getAdvancePayment());
            payment.setPaymentMethod(requestDTO.getPaymentMethod() != null ? requestDTO.getPaymentMethod() : "CASH");
            payment.setReceivedBy(requestDTO.getCreatedBy());
            paymentRepository.save(payment);
        }

        // Save history
        RepairStatusHistory history = new RepairStatusHistory();
        history.setRepairId(savedJob.getRepairId());
        history.setOldStatus(null);
        history.setNewStatus(RepairStatus.RECEIVED);
        history.setNotes("Repair job logged from POS with Advance Payment: " + requestDTO.getAdvancePayment());
        history.setChangedBy(requestDTO.getCreatedBy());
        historyRepository.save(history);

        return savedJob;
    }

    @Override
    public List<RepairJob> getAllRepairs() {
        return repairJobRepository.findAll();
    }

    @Override
    public List<RepairJob> getRepairsByBranch(Long branchId) {
        return repairJobRepository.findByBranchId(branchId);
    }

    @Override
    @Transactional
    public RepairJob finalizeRepairCost(Long repairId, BigDecimal finalCost, Long managerId) {
        RepairJob job = repairJobRepository.findById(repairId)
                .orElseThrow(() -> new RuntimeException("Repair Job not found"));

        RepairStatus oldStatus = job.getStatus();
        
        job.setFinalCost(finalCost);
        job.setStatus(RepairStatus.READY_FOR_PAYMENT);
        job.setApprovedBy(managerId);

        RepairJob savedJob = repairJobRepository.save(job);

        // Save history
        RepairStatusHistory history = new RepairStatusHistory();
        history.setRepairId(savedJob.getRepairId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(RepairStatus.READY_FOR_PAYMENT);
        history.setNotes("Cost Finalized by Manager: LKR " + finalCost);
        history.setChangedBy(managerId);
        historyRepository.save(history);

        return savedJob;
    }

    @Override
    @Transactional
    public RepairJob updateRepairStatus(Long repairId, String status, Long technicianId, String notes) {
        RepairJob job = repairJobRepository.findById(repairId)
                .orElseThrow(() -> new RuntimeException("Repair Job not found"));

        RepairStatus oldStatus = job.getStatus();
        RepairStatus newStatus = RepairStatus.valueOf(status);

        job.setStatus(newStatus);
        if (technicianId != null) {
            job.setTechnicianId(technicianId);
        }
        if (notes != null) {
            job.setDiagnosisNotes(notes);
        }

        RepairJob savedJob = repairJobRepository.save(job);

        // Save history
        RepairStatusHistory history = new RepairStatusHistory();
        history.setRepairId(savedJob.getRepairId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNotes(notes != null ? notes : "Status updated to " + status);
        history.setChangedBy(technicianId);
        historyRepository.save(history);

        return savedJob;
    }

    @Override
    @Transactional
    public RepairJob requestFinalizeCost(Long repairId, Long managerId, BigDecimal estimatedCost, String costNote) {
        RepairJob job = repairJobRepository.findById(repairId)
                .orElseThrow(() -> new RuntimeException("Repair Job not found"));

        RepairStatus oldStatus = job.getStatus();

        job.setEstimatedCost(estimatedCost);
        job.setCostNote(costNote);
        job.setStatus(RepairStatus.WAITING_APPROVAL);

        RepairJob savedJob = repairJobRepository.save(job);

        // Save history
        RepairStatusHistory history = new RepairStatusHistory();
        history.setRepairId(savedJob.getRepairId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(RepairStatus.WAITING_APPROVAL);
        history.setNotes("Payment finalize approval requested. Sent to manager/supervisor " + managerId);
        history.setChangedBy(job.getTechnicianId());
        historyRepository.save(history);

        return savedJob;
    }


    @Override
    public List<Map<String, Object>> searchRepairs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String q = query.trim();
        Set<Long> foundRepairIds = new LinkedHashSet<>();
        Map<Long, RepairJob> repairMap = new LinkedHashMap<>();

        // 1. Search by phone number (find customers first, then their repairs)
        List<Customer> customersByPhone = customerRepository.findByPhoneContaining(q);
        if (!customersByPhone.isEmpty()) {
            List<Long> customerIds = customersByPhone.stream()
                    .map(Customer::getCustomerId)
                    .collect(Collectors.toList());
            List<RepairJob> byCustomer = repairJobRepository.findByCustomerIdIn(customerIds);
            for (RepairJob rj : byCustomer) {
                foundRepairIds.add(rj.getRepairId());
                repairMap.put(rj.getRepairId(), rj);
            }
        }

        // 2. Search by device model
        List<RepairJob> byModel = repairJobRepository.findByDeviceModelContainingIgnoreCase(q);
        for (RepairJob rj : byModel) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        // 3. Search by device brand
        List<RepairJob> byBrand = repairJobRepository.findByDeviceBrandContainingIgnoreCase(q);
        for (RepairJob rj : byBrand) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        // 4. Search by repair no
        List<RepairJob> byRepairNo = repairJobRepository.findByRepairNoContainingIgnoreCase(q);
        for (RepairJob rj : byRepairNo) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        // Build enriched results with customer info
        List<Map<String, Object>> results = new ArrayList<>();
        // Build a customer lookup map
        Set<Long> allCustomerIds = repairMap.values().stream()
                .map(RepairJob::getCustomerId)
                .collect(Collectors.toSet());
        Map<Long, Customer> customerMap = new HashMap<>();
        for (Long custId : allCustomerIds) {
            customerRepository.findById(custId).ifPresent(c -> customerMap.put(custId, c));
        }

        // Also gather payment info
        for (Long repairId : foundRepairIds) {
            RepairJob job = repairMap.get(repairId);
            Customer cust = customerMap.get(job.getCustomerId());
            List<RepairPayment> payments = paymentRepository.findByRepairId(repairId);
            BigDecimal totalPaid = payments.stream()
                    .map(RepairPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("repairId", job.getRepairId());
            entry.put("repairNo", job.getRepairNo());
            entry.put("branchId", job.getBranchId());
            entry.put("customerId", job.getCustomerId());
            entry.put("customerName", cust != null ? cust.getName() : "Unknown");
            entry.put("customerPhone", cust != null ? cust.getPhone() : "");
            entry.put("deviceBrand", job.getDeviceBrand());
            entry.put("deviceModel", job.getDeviceModel());
            entry.put("imeiNo", job.getImeiNo());
            entry.put("problemDescription", job.getProblemDescription());
            entry.put("diagnosisNotes", job.getDiagnosisNotes());
            entry.put("costNote", job.getCostNote());
            entry.put("estimatedCost", job.getEstimatedCost());
            entry.put("finalCost", job.getFinalCost());
            entry.put("status", job.getStatus() != null ? job.getStatus().name() : null);
            entry.put("technicianId", job.getTechnicianId());
            entry.put("approvedBy", job.getApprovedBy());
            entry.put("createdBy", job.getCreatedBy());
            entry.put("createdAt", job.getCreatedAt());
            entry.put("updatedAt", job.getUpdatedAt());
            entry.put("totalPaid", totalPaid);
            entry.put("balanceDue", job.getFinalCost() != null ? job.getFinalCost().subtract(totalPaid) : BigDecimal.ZERO);

            results.add(entry);
        }

        return results;
    }

    @Override
    @Transactional
    public RepairJob markRepairPaid(Long repairId, BigDecimal amount, String paymentMethod, Long receivedBy) {
        RepairJob job = repairJobRepository.findById(repairId)
                .orElseThrow(() -> new RuntimeException("Repair Job not found"));

        RepairStatus oldStatus = job.getStatus();

        // Record payment
        RepairPayment payment = new RepairPayment();
        payment.setRepairId(repairId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        payment.setReceivedBy(receivedBy);
        paymentRepository.save(payment);

        // Step 1: Mark as PAID and log history
        job.setStatus(RepairStatus.PAID);
        repairJobRepository.save(job);

        RepairStatusHistory paidHistory = new RepairStatusHistory();
        paidHistory.setRepairId(job.getRepairId());
        paidHistory.setOldStatus(oldStatus);
        paidHistory.setNewStatus(RepairStatus.PAID);
        paidHistory.setNotes("Payment collected: LKR " + amount + " via " + (paymentMethod != null ? paymentMethod : "CASH"));
        paidHistory.setChangedBy(receivedBy);
        historyRepository.save(paidHistory);

        // Step 2: Immediately mark as DELIVERED (device given to customer)
        job.setStatus(RepairStatus.DELIVERED);
        RepairJob savedJob = repairJobRepository.save(job);

        RepairStatusHistory deliveredHistory = new RepairStatusHistory();
        deliveredHistory.setRepairId(job.getRepairId());
        deliveredHistory.setOldStatus(RepairStatus.PAID);
        deliveredHistory.setNewStatus(RepairStatus.DELIVERED);
        deliveredHistory.setNotes("Device delivered to customer after payment.");
        deliveredHistory.setChangedBy(receivedBy);
        historyRepository.save(deliveredHistory);

        return savedJob;
    }
}



