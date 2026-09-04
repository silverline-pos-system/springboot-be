package com.silverline.erp.module.procurement.service.impl;

import com.silverline.erp.domain.procurement.Grn;
import com.silverline.erp.domain.procurement.GrnPaymentRequest;
import com.silverline.erp.domain.procurement.Supplier;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.procurement.dto.GrnPaymentRequestDTO;
import com.silverline.erp.module.procurement.repository.GrnPaymentRequestRepository;
import com.silverline.erp.module.procurement.repository.GrnRepository;
import com.silverline.erp.module.procurement.service.GrnPaymentRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrnPaymentRequestServiceImpl implements GrnPaymentRequestService {

    private final GrnPaymentRequestRepository paymentRequestRepository;
    private final GrnRepository grnRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public GrnPaymentRequestDTO createPaymentRequest(Long grnId, Long requestedBy) {
        log.info("Creating payment request for GRN ID: {}", grnId);

        if (paymentRequestRepository.findByGrnId(grnId).isPresent()) {
            throw new RuntimeException("Payment request already exists for this GRN");
        }

        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new RuntimeException("GRN not found: " + grnId));

        if (!"POSTED".equals(grn.getStatus())) {
            throw new RuntimeException("Can only create payment request for posted GRNs");
        }

        Supplier supplier = supplierRepository.findById(grn.getSupplierId()).orElse(null);

        GrnPaymentRequest request = new GrnPaymentRequest();
        request.setGrnId(grnId);
        request.setGrnNo(grn.getGrnNo());
        request.setBranchId(grn.getBranchId());
        request.setSupplierId(grn.getSupplierId());
        request.setSupplierName(supplier != null ? supplier.getName() : "Unknown Supplier");
        request.setAmount(grn.getNetAmount());
        request.setInvoiceNo(grn.getInvoiceNo());
        request.setStatus("PENDING");
        request.setPriority("NORMAL");
        request.setRequestedBy(requestedBy);

        if (grn.getInvoiceDate() != null) {
            request.setDueDate(grn.getInvoiceDate().atStartOfDay().plusDays(30));
        } else {
            request.setDueDate(LocalDateTime.now().plusDays(30));
        }

        request = paymentRequestRepository.save(request);
        log.info("Payment request created with ID: {}", request.getRequestId());
        return convertToDTO(request);
    }

    @Override
    public List<GrnPaymentRequestDTO> getPaymentRequestsByBranch(Long branchId) {
        return paymentRequestRepository.findByBranchId(branchId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public Long getPendingCountByBranch(Long branchId) {
        return paymentRequestRepository.countPendingByBranch(branchId);
    }

    @Override
    public List<GrnPaymentRequestDTO> getPaymentRequestsByStatus(String status) {
        return paymentRequestRepository.findByStatus(status).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<GrnPaymentRequestDTO> getManagerPaymentRequests() {
        return paymentRequestRepository.findManagerPendingRequests().stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public Long getManagerPendingCount() {
        return paymentRequestRepository.countManagerPending();
    }

    @Override
    @Transactional
    public GrnPaymentRequestDTO transferToManager(Long requestId, TransferToManagerRequest request, Long transferredBy) {
        verifySupervisorCredentials(request.getSupervisorUsername(), request.getSupervisorPassword());

        GrnPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        if (!"PENDING".equals(paymentRequest.getStatus()) && !"SUPERVISOR_APPROVED".equals(paymentRequest.getStatus())) {
            throw new RuntimeException("Cannot transfer request in current status: " + paymentRequest.getStatus());
        }

        paymentRequest.setStatus("TRANSFERRED_TO_MANAGER");
        paymentRequest.setSupervisorApprovedBy(transferredBy);
        paymentRequest.setSupervisorApprovedAt(LocalDateTime.now());
        paymentRequest.setTransferredToManagerBy(transferredBy);
        paymentRequest.setTransferredAt(LocalDateTime.now());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = paymentRequest.getNotes() != null ? paymentRequest.getNotes() + "\n" : "";
            paymentRequest.setNotes(existingNotes + "Transfer Note: " + request.getNotes());
        }
        if (request.getPriority() != null) {
            paymentRequest.setPriority(request.getPriority());
        }

        paymentRequest = paymentRequestRepository.save(paymentRequest);
        return convertToDTO(paymentRequest);
    }

    @Override
    @Transactional
    public GrnPaymentRequestDTO processPayment(Long requestId, ProcessPaymentRequest request, Long processedBy) {
        GrnPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        if (!"TRANSFERRED_TO_MANAGER".equals(paymentRequest.getStatus()) && !"PROCESSING".equals(paymentRequest.getStatus())) {
            throw new RuntimeException("Cannot process payment in current status: " + paymentRequest.getStatus());
        }

        paymentRequest.setStatus("PAID");
        paymentRequest.setProcessedBy(processedBy);
        paymentRequest.setProcessedAt(LocalDateTime.now());
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setPaymentReference(request.getPaymentReference());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = paymentRequest.getNotes() != null ? paymentRequest.getNotes() + "\n" : "";
            paymentRequest.setNotes(existingNotes + "Payment Note: " + request.getNotes());
        }

        Grn grn = grnRepository.findById(paymentRequest.getGrnId()).orElse(null);
        if (grn != null) {
            grn.setPaymentStatus("PAID");
            grnRepository.save(grn);
        }

        paymentRequest = paymentRequestRepository.save(paymentRequest);
        return convertToDTO(paymentRequest);
    }

    @Override
    @Transactional
    public GrnPaymentRequestDTO rejectRequest(Long requestId, String reason, Long rejectedBy) {
        GrnPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        paymentRequest.setStatus("REJECTED");
        paymentRequest.setProcessedBy(rejectedBy);
        paymentRequest.setProcessedAt(LocalDateTime.now());

        String existingNotes = paymentRequest.getNotes() != null ? paymentRequest.getNotes() + "\n" : "";
        paymentRequest.setNotes(existingNotes + "Rejection Reason: " + reason);

        Grn grn = grnRepository.findById(paymentRequest.getGrnId()).orElse(null);
        if (grn != null) {
            grn.setPaymentStatus("REJECTED");
            grnRepository.save(grn);
        }

        paymentRequest = paymentRequestRepository.save(paymentRequest);
        return convertToDTO(paymentRequest);
    }

    @Override
    public GrnPaymentRequestDTO getPaymentRequestById(Long requestId) {
        return paymentRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));
    }

    private void verifySupervisorCredentials(String username, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            if (!auth.isAuthenticated()) {
                throw new RuntimeException("Invalid supervisor credentials");
            }
            UserProfile supervisor = userProfileRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Supervisor not found"));
            String role = supervisor.getRole().name();
            if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPERVISOR".equals(role)) {
                throw new RuntimeException("User does not have supervisor privileges");
            }
        } catch (Exception e) {
            throw new RuntimeException("Supervisor authorization failed: " + e.getMessage());
        }
    }

    private GrnPaymentRequestDTO convertToDTO(GrnPaymentRequest request) {
        GrnPaymentRequestDTO dto = GrnPaymentRequestDTO.builder()
                .requestId(request.getRequestId())
                .grnId(request.getGrnId())
                .grnNo(request.getGrnNo())
                .branchId(request.getBranchId())
                .supplierId(request.getSupplierId())
                .supplierName(request.getSupplierName())
                .amount(request.getAmount())
                .invoiceNo(request.getInvoiceNo())
                .status(request.getStatus())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .requestedBy(request.getRequestedBy())
                .supervisorApprovedBy(request.getSupervisorApprovedBy())
                .supervisorApprovedAt(request.getSupervisorApprovedAt())
                .transferredToManagerBy(request.getTransferredToManagerBy())
                .transferredAt(request.getTransferredAt())
                .processedBy(request.getProcessedBy())
                .processedAt(request.getProcessedAt())
                .paymentReference(request.getPaymentReference())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();

        branchRepository.findById(request.getBranchId()).ifPresent(branch -> dto.setBranchName(branch.getName()));
        if (request.getRequestedBy() != null) {
            userProfileRepository.findById(request.getRequestedBy()).ifPresent(user -> dto.setRequestedByName(user.getFullName()));
        }
        if (request.getSupervisorApprovedBy() != null) {
            userProfileRepository.findById(request.getSupervisorApprovedBy()).ifPresent(user -> dto.setSupervisorApprovedByName(user.getFullName()));
        }
        if (request.getProcessedBy() != null) {
            userProfileRepository.findById(request.getProcessedBy()).ifPresent(user -> dto.setProcessedByName(user.getFullName()));
        }
        return dto;
    }
}
