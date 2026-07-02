package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository("inventoryDispatchRepository")
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    Optional<Dispatch> findByDispatchNo(String dispatchNo);
    List<Dispatch> findByBranchId(Long branchId);
    List<Dispatch> findBySupplierId(Long supplierId);
    List<Dispatch> findByPoId(Long poId);
    List<Dispatch> findByStatus(String status);
    List<Dispatch> findByBranchIdAndStatus(Long branchId, String status);
    List<Dispatch> findByPaymentStatus(String paymentStatus);
    List<Dispatch> findByBranchIdAndDispatchDateBetween(Long branchId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT g FROM Dispatch g WHERE " +
            "(:branchId IS NULL OR g.branchId = :branchId) AND " +
            "(:supplierId IS NULL OR g.supplierId = :supplierId) AND " +
            "(:status IS NULL OR g.status = :status) AND " +
            "(:paymentStatus IS NULL OR g.paymentStatus = :paymentStatus) AND " +
            "(:startDate IS NULL OR g.dispatchDate >= :startDate) AND " +
            "(:endDate IS NULL OR g.dispatchDate <= :endDate) AND " +
            "(:dispatchNo IS NULL OR lower(g.dispatchNo) LIKE lower(concat('%', :dispatchNo, '%'))) AND " +
            "(:invoiceNo IS NULL OR lower(g.invoiceNo) LIKE lower(concat('%', :invoiceNo, '%')))")
    List<Dispatch> findByFilters(
            @Param("branchId") Long branchId,
            @Param("supplierId") Long supplierId,
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dispatchNo") String dispatchNo,
            @Param("invoiceNo") String invoiceNo
    );

    long countByBranchIdAndDispatchDate(Long branchId, LocalDate dispatchDate);

    // SUPER_ADMIN QUERIES
    Long countByBranchIdAndStatus(Long branchId, String status);

    @Query(value = """
                                SELECT g.dispatch_id, g.dispatch_no, g.dispatch_date, g.status, g.total_amount,
               s.name as supplier_name,
                                        (SELECT COUNT(*) FROM item_dispatch_lines dl WHERE dl.dispatch_id = g.dispatch_id) as item_count
                                FROM item_dispatches g
            JOIN suppliers s ON g.supplier_id = s.supplier_id
            WHERE g.status = 'PENDING'
            ORDER BY g.created_at DESC
            """, nativeQuery = true)
    List<Object[]> findAllPendingDispatchesWithDetails();

    Long countByStatus(String status);
}


