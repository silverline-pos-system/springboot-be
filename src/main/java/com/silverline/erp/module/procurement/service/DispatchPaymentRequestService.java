package com.silverline.erp.module.procurement.service;

import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.module.procurement.dto.DispatchPaymentRequestDTO;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;

import java.util.List;

public interface DispatchPaymentRequestService {
    
    /**
     * Create a payment request when Dispatch is approved
     */
    DispatchPaymentRequestDTO createPaymentRequest(Long dispatchId, Long requestedBy);
    
    /**
     * Get all payment requests for a branch (for POS display)
     */
    List<DispatchPaymentRequestDTO> getPaymentRequestsByBranch(Long branchId);
    
    /**
     * Get pending payment requests count for POS notification
     */
    Long getPendingCountByBranch(Long branchId);
    
    /**
     * Get payment requests with specific status
     */
    List<DispatchPaymentRequestDTO> getPaymentRequestsByStatus(String status);
    
    /**
     * Get payment requests for manager (transferred ones)
     */
    List<DispatchPaymentRequestDTO> getManagerPaymentRequests();
    
    /**
     * Get manager pending count for notification
     */
    Long getManagerPendingCount();
    
    /**
     * Transfer payment request to manager with supervisor approval
     */
    DispatchPaymentRequestDTO transferToManager(Long requestId, TransferToManagerRequest request, Long transferredBy);
    
    /**
     * Manager processes the payment
     */
    DispatchPaymentRequestDTO processPayment(Long requestId, ProcessPaymentRequest request, Long processedBy);
    
    /**
     * Reject a payment request
     */
    DispatchPaymentRequestDTO rejectRequest(Long requestId, String reason, Long rejectedBy);
    
    /**
     * Get single payment request by ID
     */
    DispatchPaymentRequestDTO getPaymentRequestById(Long requestId);
}

