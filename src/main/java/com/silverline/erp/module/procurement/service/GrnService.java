package com.silverline.erp.module.procurement.service;

import com.silverline.erp.module.procurement.dto.GrnCreateRequestDTO;
import com.silverline.erp.module.procurement.dto.GrnFilterDTO;
import com.silverline.erp.module.procurement.dto.GrnItemDTO;
import com.silverline.erp.module.procurement.dto.GrnResponseDTO;

import java.util.List;

public interface GrnService {

    GrnResponseDTO createGrn(GrnCreateRequestDTO request, Long currentUserId);

    GrnResponseDTO getGrnById(Long grnId);

    List<GrnResponseDTO> getGrnsByBranch(Long branchId);

    List<GrnResponseDTO> searchGrns(GrnFilterDTO filter);

    /** Post (confirm) a GRN: update stock, per-branch price, PO received qty, publish event. */
    GrnResponseDTO postGrn(Long grnId, Long postedBy);

    GrnResponseDTO updatePaymentStatus(Long grnId, String paymentStatus);

    void deleteGrn(Long grnId);

    GrnResponseDTO cancelGrn(Long grnId, Long cancelledBy, String reason);

    List<GrnItemDTO> getGrnItemsByProduct(Long productId, Long branchId);

    boolean isGrnNumberExists(String grnNo);
}
