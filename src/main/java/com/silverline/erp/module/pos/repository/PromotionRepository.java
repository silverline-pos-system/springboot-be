package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByBranchId(Long branchId);

    /**
     * Active promotions that apply to a branch (its own plus all-branch ones) and
     * are within their time window, highest priority first.
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
            "AND (p.branchId IS NULL OR p.branchId = :branchId) " +
            "AND (p.startAt IS NULL OR p.startAt <= :now) " +
            "AND (p.endAt IS NULL OR p.endAt >= :now) " +
            "AND (p.maxUses IS NULL OR p.usesCount < p.maxUses) " +
            "ORDER BY p.priority DESC, p.promotionId ASC")
    List<Promotion> findActiveForBranch(@Param("branchId") Long branchId, @Param("now") LocalDateTime now);
}
