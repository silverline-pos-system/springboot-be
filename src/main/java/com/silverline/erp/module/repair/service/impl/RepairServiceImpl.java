package com.silverline.erp.module.repair.service.impl;

import com.silverline.erp.domain.enums.RepairStatus;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.domain.repair.RepairJob;
import com.silverline.erp.domain.repair.RepairPayment;
import com.silverline.erp.domain.repair.RepairStatusHistory;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.repair.dto.RepairJobRequestDTO;
import com.silverline.erp.module.repair.repository.RepairJobRepository;
import com.silverline.erp.module.repair.repository.RepairPaymentRepository;
import com.silverline.erp.module.repair.repository.RepairStatusHistoryRepository;
import com.silverline.erp.module.repair.service.RepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairServiceImpl implements RepairService {

    private final RepairJobRepository repairJobRepository;
    private final RepairStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final RepairPaymentRepository paymentRepository;

    private Pageable capPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());
    }

    @Override
    @Transactional
    public RepairJob logRepairJob(RepairJobRequestDTO requestDTO) {
        Customer customer = customerRepository.findByPhone(requestDTO.getContactNo())
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setName(requestDTO.getCustomerName());
                    newCustomer.setPhone(requestDTO.getContactNo());
                    return customerRepository.save(newCustomer);
                });

        RepairJob job = new RepairJob();
        job.setRepairNo("REP-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000));
        job.setBranchId(requestDTO.getBranchId() != null ? requestDTO.getBranchId() : 1L);
        job.setCustomerId(customer.getCustomerId());
        job.setDeviceBrand(requestDTO.getDeviceBrand());
        job.setDeviceModel(requestDTO.getDeviceModel());
        job.setImeiNo(requestDTO.getImeiNo());
        job.setProblemDescription(requestDTO.getProblemDescription());
        job.setStatus(RepairStatus.RECEIVED);
        job.setCreatedBy(requestDTO.getCreatedBy());

        RepairJob savedJob = repairJobRepository.save(job);

        if (requestDTO.getAdvancePayment() != null && requestDTO.getAdvancePayment().compareTo(BigDecimal.ZERO) > 0) {
            RepairPayment payment = new RepairPayment();
            payment.setRepairId(savedJob.getRepairId());
            payment.setAmount(requestDTO.getAdvancePayment());
            payment.setPaymentMethod(requestDTO.getPaymentMethod() != null ? requestDTO.getPaymentMethod() : "CASH");
            payment.setReceivedBy(requestDTO.getCreatedBy());
            paymentRepository.save(payment);
        }

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
    public Page<RepairJob> getAllRepairs(Pageable pageable) {
        return repairJobRepository.findAll(capPageable(pageable));
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

        List<RepairJob> byModel = repairJobRepository.findByDeviceModelContainingIgnoreCase(q);
        for (RepairJob rj : byModel) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        List<RepairJob> byBrand = repairJobRepository.findByDeviceBrandContainingIgnoreCase(q);
        for (RepairJob rj : byBrand) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        List<RepairJob> byRepairNo = repairJobRepository.findByRepairNoContainingIgnoreCase(q);
        for (RepairJob rj : byRepairNo) {
            foundRepairIds.add(rj.getRepairId());
            repairMap.put(rj.getRepairId(), rj);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Set<Long> allCustomerIds = repairMap.values().stream()
                .map(RepairJob::getCustomerId)
                .collect(Collectors.toSet());
        Map<Long, Customer> customerMap = new HashMap<>();
        for (Long custId : allCustomerIds) {
            customerRepository.findById(custId).ifPresent(c -> customerMap.put(custId, c));
        }

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

        RepairPayment payment = new RepairPayment();
        payment.setRepairId(repairId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        payment.setReceivedBy(receivedBy);
        paymentRepository.save(payment);

        job.setStatus(RepairStatus.PAID);
        repairJobRepository.save(job);

        RepairStatusHistory paidHistory = new RepairStatusHistory();
        paidHistory.setRepairId(job.getRepairId());
        paidHistory.setOldStatus(oldStatus);
        paidHistory.setNewStatus(RepairStatus.PAID);
        paidHistory.setNotes("Payment collected: LKR " + amount + " via " + (paymentMethod != null ? paymentMethod : "CASH"));
        paidHistory.setChangedBy(receivedBy);
        historyRepository.save(paidHistory);

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
