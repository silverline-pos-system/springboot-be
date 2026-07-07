package com.silverline.erp.common.audit.repository;

import com.silverline.erp.domain.audit.BranchActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BranchActivityRepository extends JpaRepository<BranchActivity, Long> {

    List<BranchActivity> findByBranchIdOrderByTimestampDesc(Long branchId);

    // For pagination and filtering
    Page<BranchActivity> findByBranchId(Long branchId, Pageable pageable);

    List<BranchActivity> findByBranchIdAndTimestampBetweenOrderByTimestampDesc(
            Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    // Find recent activities for dashboard
    List<BranchActivity> findTop20ByBranchIdOrderByTimestampDesc(Long branchId);

    List<BranchActivity> findByActionType(String actionType);

    List<BranchActivity> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<BranchActivity> findByActionTypeAndTimestampBetween(String actionType, LocalDateTime startDate, LocalDateTime endDate);

    List<BranchActivity> findByBranchId(Long branchId);

    List<BranchActivity> findByBranchIdAndActionType(Long branchId, String actionType);

    List<BranchActivity> findByBranchIdAndTimestampBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    List<BranchActivity> findByBranchIdAndActionTypeAndTimestampBetween(Long branchId, String actionType,
                                                                        LocalDateTime startDate, LocalDateTime endDate);
}
