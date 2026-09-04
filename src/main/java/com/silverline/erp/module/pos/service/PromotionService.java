package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.pos.dto.PromotionEval;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    /** Evaluate all active promotions for a branch against the given cart lines. */
    PromotionEval.Outcome evaluate(List<PromotionEval.Line> lines, Long branchId, LocalDateTime now);

    /** Record that the promotions in an outcome were applied to a sale (audit + usage count). */
    void recordUsage(PromotionEval.Outcome outcome, Long saleId);
}
