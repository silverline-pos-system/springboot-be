package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    List<PromotionUsage> findBySaleId(Long saleId);
    List<PromotionUsage> findByPromotionId(Long promotionId);
}
