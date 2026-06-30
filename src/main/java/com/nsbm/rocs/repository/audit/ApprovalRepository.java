package com.nsbm.rocs.repository.audit;

import com.nsbm.rocs.entity.audit.Approval;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByBranchId(Long branchId);
    List<Approval> findByStatus(String status);
    List<Approval> findByRequestedBy(Long userId);
    List<Approval> findByTypeAndStatus(String type, String status);
    List<Approval> findByReferenceIdAndType(Long referenceId, String type);
    List<Approval> findByBranchIdAndStatus(Long branchId, String status);
    List<Approval> findByBranchIdIn(List<Long> branchIds);
    List<Approval> findByBranchIdInAndStatus(List<Long> branchIds, String status);
}

