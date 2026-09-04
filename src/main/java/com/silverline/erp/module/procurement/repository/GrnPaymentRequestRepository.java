package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.GrnPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrnPaymentRequestRepository extends JpaRepository<GrnPaymentRequest, Long> {

    List<GrnPaymentRequest> findByBranchId(Long branchId);

    List<GrnPaymentRequest> findByStatus(String status);

    List<GrnPaymentRequest> findByBranchIdAndStatus(Long branchId, String status);

    Optional<GrnPaymentRequest> findByGrnId(Long grnId);

    @Query("SELECT r FROM GrnPaymentRequest r WHERE r.branchId = :branchId AND r.status IN :statuses ORDER BY r.createdAt DESC")
    List<GrnPaymentRequest> findByBranchIdAndStatusIn(@Param("branchId") Long branchId, @Param("statuses") List<String> statuses);

    @Query("SELECT r FROM GrnPaymentRequest r WHERE r.status IN ('PENDING', 'SUPERVISOR_APPROVED') AND r.branchId = :branchId ORDER BY r.priority DESC, r.createdAt ASC")
    List<GrnPaymentRequest> findPendingRequestsByBranch(@Param("branchId") Long branchId);

    @Query("SELECT r FROM GrnPaymentRequest r WHERE r.status IN ('TRANSFERRED_TO_MANAGER', 'PROCESSING') ORDER BY r.priority DESC, r.transferredAt ASC")
    List<GrnPaymentRequest> findManagerPendingRequests();

    @Query("SELECT COUNT(r) FROM GrnPaymentRequest r WHERE r.branchId = :branchId AND r.status IN ('PENDING', 'SUPERVISOR_APPROVED')")
    Long countPendingByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(r) FROM GrnPaymentRequest r WHERE r.status IN ('TRANSFERRED_TO_MANAGER', 'PROCESSING')")
    Long countManagerPending();
}
