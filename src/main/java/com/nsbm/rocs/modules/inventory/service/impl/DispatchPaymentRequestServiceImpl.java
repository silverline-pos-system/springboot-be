package com.nsbm.rocs.modules.inventory.service.impl;

import com.nsbm.rocs.modules.auth.repo.UserProfileRepo;
import com.nsbm.rocs.entity.inventory.Dispatch;
import com.nsbm.rocs.entity.inventory.DispatchPaymentRequest;
import com.nsbm.rocs.entity.inventory.Supplier;
import com.nsbm.rocs.entity.main.UserProfile;
import com.nsbm.rocs.modules.inventory.dto.DispatchPaymentRequestDTO;
import com.nsbm.rocs.modules.inventory.dto.ProcessPaymentRequest;
import com.nsbm.rocs.modules.inventory.dto.TransferToManagerRequest;
import com.nsbm.rocs.modules.inventory.repository.DispatchPaymentRequestRepository;
import com.nsbm.rocs.modules.inventory.repository.DispatchRepository;
import com.nsbm.rocs.modules.inventory.repository.SupplierRepository;
import com.nsbm.rocs.modules.inventory.service.DispatchPaymentRequestService;
import com.nsbm.rocs.repository.BranchRepository;
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
public class DispatchPaymentRequestServiceImpl implements DispatchPaymentRequestService {

    private final DispatchPaymentRequestRepository paymentRequestRepository;
    private final DispatchRepository dispatchRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserProfileRepo userProfileRepo;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public DispatchPaymentRequestDTO createPaymentRequest(Long dispatchId, Long requestedBy) {
        log.info("Creating payment request for Dispatch ID: {}", dispatchId);

        // Check if request already exists
        if (paymentRequestRepository.findByDispatchId(dispatchId).isPresent()) {
            throw new RuntimeException("Payment request already exists for this Dispatch");
        }

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new RuntimeException("Dispatch not found: " + dispatchId));

        if (!"APPROVED".equals(dispatch.getStatus())) {
            throw new RuntimeException("Can only create payment request for approved Dispatches");
        }

        Supplier supplier = supplierRepository.findById(dispatch.getSupplierId())
                .orElse(null);

        DispatchPaymentRequest request = new DispatchPaymentRequest();
        request.setDispatchId(dispatchId);
        request.setDispatchNo(dispatch.getDispatchNo());
        request.setBranchId(dispatch.getBranchId());
        request.setSupplierId(dispatch.getSupplierId());
        request.setSupplierName(supplier != null ? supplier.getName() : "Unknown Supplier");
        request.setAmount(dispatch.getNetAmount());
        request.setInvoiceNo(dispatch.getInvoiceNo());
        request.setStatus("PENDING");
        request.setPriority("NORMAL");
        request.setRequestedBy(requestedBy);

        // Set due date (e.g., 30 days from invoice date or now)
        if (dispatch.getInvoiceDate() != null) {
            request.setDueDate(dispatch.getInvoiceDate().atStartOfDay().plusDays(30));
        } else {
            request.setDueDate(LocalDateTime.now().plusDays(30));
        }

        request = paymentRequestRepository.save(request);
        log.info("Payment request created with ID: {}", request.getRequestId());

        return convertToDTO(request);
    }

    @Override
    public List<DispatchPaymentRequestDTO> getPaymentRequestsByBranch(Long branchId) {
        return paymentRequestRepository.findByBranchId(branchId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Long getPendingCountByBranch(Long branchId) {
        return paymentRequestRepository.countPendingByBranch(branchId);
    }

    @Override
    public List<DispatchPaymentRequestDTO> getPaymentRequestsByStatus(String status) {
        return paymentRequestRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DispatchPaymentRequestDTO> getManagerPaymentRequests() {
        return paymentRequestRepository.findManagerPendingRequests().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Long getManagerPendingCount() {
        return paymentRequestRepository.countManagerPending();
    }

    @Override
    @Transactional
    public DispatchPaymentRequestDTO transferToManager(Long requestId, TransferToManagerRequest request, Long transferredBy) {
        log.info("Transferring payment request {} to manager", requestId);

        // Verify supervisor credentials
        verifySupervisorCredentials(request.getSupervisorUsername(), request.getSupervisorPassword());

        DispatchPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        if (!"PENDING".equals(paymentRequest.getStatus()) && !"SUPERVISOR_APPROVED".equals(paymentRequest.getStatus())) {
            throw new RuntimeException("Cannot transfer request in current status: " + paymentRequest.getStatus());
        }

        // Update status and tracking
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
        log.info("Payment request {} transferred to manager", requestId);

        return convertToDTO(paymentRequest);
    }

    @Override
    @Transactional
    public DispatchPaymentRequestDTO processPayment(Long requestId, ProcessPaymentRequest request, Long processedBy) {
        log.info("Processing payment for request {}", requestId);

        DispatchPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        if (!"TRANSFERRED_TO_MANAGER".equals(paymentRequest.getStatus()) && !"PROCESSING".equals(paymentRequest.getStatus())) {
            throw new RuntimeException("Cannot process payment in current status: " + paymentRequest.getStatus());
        }

        // Update payment details
        paymentRequest.setStatus("PAID");
        paymentRequest.setProcessedBy(processedBy);
        paymentRequest.setProcessedAt(LocalDateTime.now());
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setPaymentReference(request.getPaymentReference());
        
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = paymentRequest.getNotes() != null ? paymentRequest.getNotes() + "\n" : "";
            paymentRequest.setNotes(existingNotes + "Payment Note: " + request.getNotes());
        }

        // Update Dispatch payment status
        Dispatch dispatch = dispatchRepository.findById(paymentRequest.getDispatchId()).orElse(null);
        if (dispatch != null) {
            dispatch.setPaymentStatus("PAID");
            dispatchRepository.save(dispatch);
        }

        paymentRequest = paymentRequestRepository.save(paymentRequest);
        log.info("Payment processed for request {}", requestId);

        return convertToDTO(paymentRequest);
    }

    @Override
    @Transactional
    public DispatchPaymentRequestDTO rejectRequest(Long requestId, String reason, Long rejectedBy) {
        log.info("Rejecting payment request {}", requestId);

        DispatchPaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));

        paymentRequest.setStatus("REJECTED");
        paymentRequest.setProcessedBy(rejectedBy);
        paymentRequest.setProcessedAt(LocalDateTime.now());
        
        String existingNotes = paymentRequest.getNotes() != null ? paymentRequest.getNotes() + "\n" : "";
        paymentRequest.setNotes(existingNotes + "Rejection Reason: " + reason);

        // Update Dispatch payment status
        Dispatch dispatch = dispatchRepository.findById(paymentRequest.getDispatchId()).orElse(null);
        if (dispatch != null) {
            dispatch.setPaymentStatus("REJECTED");
            dispatchRepository.save(dispatch);
        }

        paymentRequest = paymentRequestRepository.save(paymentRequest);
        log.info("Payment request {} rejected", requestId);

        return convertToDTO(paymentRequest);
    }

    @Override
    public DispatchPaymentRequestDTO getPaymentRequestById(Long requestId) {
        return paymentRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + requestId));
    }

    private void verifySupervisorCredentials(String username, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            if (!auth.isAuthenticated()) {
                throw new RuntimeException("Invalid supervisor credentials");
            }

            UserProfile supervisor = userProfileRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Supervisor not found"));

            String role = supervisor.getRole().name();
            if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPERVISOR".equals(role)) {
                throw new RuntimeException("User does not have supervisor privileges");
            }
        } catch (Exception e) {
            throw new RuntimeException("Supervisor authorization failed: " + e.getMessage());
        }
    }

    private DispatchPaymentRequestDTO convertToDTO(DispatchPaymentRequest request) {
        DispatchPaymentRequestDTO dto = DispatchPaymentRequestDTO.builder()
                .requestId(request.getRequestId())
                .dispatchId(request.getDispatchId())
                .dispatchNo(request.getDispatchNo())
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

        // Fetch branch name
        branchRepository.findById(request.getBranchId()).ifPresent(branch ->
                dto.setBranchName(branch.getName()));

        // Fetch user names
        if (request.getRequestedBy() != null) {
            userProfileRepo.findById(request.getRequestedBy()).ifPresent(user ->
                    dto.setRequestedByName(user.getFullName()));
        }
        if (request.getSupervisorApprovedBy() != null) {
            userProfileRepo.findById(request.getSupervisorApprovedBy()).ifPresent(user ->
                    dto.setSupervisorApprovedByName(user.getFullName()));
        }
        if (request.getProcessedBy() != null) {
            userProfileRepo.findById(request.getProcessedBy()).ifPresent(user ->
                    dto.setProcessedByName(user.getFullName()));
        }

        return dto;
    }
}
