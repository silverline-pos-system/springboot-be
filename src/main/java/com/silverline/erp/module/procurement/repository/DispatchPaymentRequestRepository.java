package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.inventory.DispatchPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchPaymentRequestRepository extends JpaRepository<DispatchPaymentRequest, Long> {

    List<DispatchPaymentRequest> findByBranchId(Long branchId);

    List<DispatchPaymentRequest> findByStatus(String status);

    List<DispatchPaymentRequest> findByBranchIdAndStatus(Long branchId, String status);

    Optional<DispatchPaymentRequest> findByDispatchId(Long dispatchId);

    @Query("SELECT r FROM DispatchPaymentRequest r WHERE r.branchId = :branchId AND r.status IN :statuses ORDER BY r.createdAt DESC")
    List<DispatchPaymentRequest> findByBranchIdAndStatusIn(@Param("branchId") Long branchId, @Param("statuses") List<String> statuses);

    @Query("SELECT r FROM DispatchPaymentRequest r WHERE r.status IN ('PENDING', 'SUPERVISOR_APPROVED') AND r.branchId = :branchId ORDER BY r.priority DESC, r.createdAt ASC")
    List<DispatchPaymentRequest> findPendingRequestsByBranch(@Param("branchId") Long branchId);

    @Query("SELECT r FROM DispatchPaymentRequest r WHERE r.status IN ('TRANSFERRED_TO_MANAGER', 'PROCESSING') ORDER BY r.priority DESC, r.transferredAt ASC")
    List<DispatchPaymentRequest> findManagerPendingRequests();

    @Query("SELECT COUNT(r) FROM DispatchPaymentRequest r WHERE r.branchId = :branchId AND r.status IN ('PENDING', 'SUPERVISOR_APPROVED')")
    Long countPendingByBranch(@Param("branchId") Long branchId);
    
    @Query("SELECT COUNT(r) FROM DispatchPaymentRequest r WHERE r.status IN ('TRANSFERRED_TO_MANAGER', 'PROCESSING')")
    Long countManagerPending();
}

