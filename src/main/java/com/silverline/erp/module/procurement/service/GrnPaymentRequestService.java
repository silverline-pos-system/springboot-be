package com.silverline.erp.module.procurement.service;

import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.procurement.dto.GrnPaymentRequestDTO;

import java.util.List;

public interface GrnPaymentRequestService {

    /** Create a payment request when a GRN is posted. */
    GrnPaymentRequestDTO createPaymentRequest(Long grnId, Long requestedBy);

    List<GrnPaymentRequestDTO> getPaymentRequestsByBranch(Long branchId);

    Long getPendingCountByBranch(Long branchId);

    List<GrnPaymentRequestDTO> getPaymentRequestsByStatus(String status);

    List<GrnPaymentRequestDTO> getManagerPaymentRequests();

    Long getManagerPendingCount();

    GrnPaymentRequestDTO transferToManager(Long requestId, TransferToManagerRequest request, Long transferredBy);

    GrnPaymentRequestDTO processPayment(Long requestId, ProcessPaymentRequest request, Long processedBy);

    GrnPaymentRequestDTO rejectRequest(Long requestId, String reason, Long rejectedBy);

    GrnPaymentRequestDTO getPaymentRequestById(Long requestId);
}
