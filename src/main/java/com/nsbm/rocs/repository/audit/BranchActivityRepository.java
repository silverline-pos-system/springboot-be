package com.nsbm.rocs.repository.audit;

import com.nsbm.rocs.entity.audit.BranchActivity;
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
}
