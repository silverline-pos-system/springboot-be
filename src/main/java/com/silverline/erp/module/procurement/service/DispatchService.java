package com.silverline.erp.module.procurement.service;

import com.silverline.erp.module.procurement.dto.*;

import java.util.List;

public interface DispatchService {

    DispatchResponseDTO createDispatch(DispatchCreateRequestDTO request, Long currentUserId);

    DispatchResponseDTO getDispatchById(Long dispatchId);

    List<DispatchResponseDTO> getDispatchesByBranch(Long branchId);

    List<DispatchResponseDTO> searchDispatches(DispatchFilterDTO filter);

    DispatchResponseDTO updateDispatch(Long dispatchId, DispatchUpdateRequestDTO request);

    DispatchResponseDTO approveDispatch(Long dispatchId, Long approvedBy);

    DispatchResponseDTO updatePaymentStatus(Long dispatchId, String paymentStatus);

    void deleteDispatch(Long dispatchId);

    DispatchStatsDTO getDispatchStats(Long branchId, String period);

    DispatchResponseDTO rejectDispatch(Long dispatchId, Long rejectedBy, String reason);

    List<DispatchItemDTO> getDispatchItemsByProduct(Long productId, Long branchId);

    boolean isDispatchNumberExists(String dispatchNo);
}

