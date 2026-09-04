package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.Grn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrnRepository extends JpaRepository<Grn, Long> {

    Optional<Grn> findByGrnNo(String grnNo);

    List<Grn> findByBranchId(Long branchId);

    List<Grn> findBySupplierId(Long supplierId);

    List<Grn> findByPoId(Long poId);

    List<Grn> findByStatus(String status);

    List<Grn> findByBranchIdAndStatus(Long branchId, String status);

    List<Grn> findByPaymentStatus(String paymentStatus);

    List<Grn> findByBranchIdAndGrnDateBetween(Long branchId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT g FROM Grn g WHERE " +
            "(:branchId IS NULL OR g.branchId = :branchId) AND " +
            "(:supplierId IS NULL OR g.supplierId = :supplierId) AND " +
            "(:status IS NULL OR g.status = :status) AND " +
            "(:paymentStatus IS NULL OR g.paymentStatus = :paymentStatus) AND " +
            "(:startDate IS NULL OR g.grnDate >= :startDate) AND " +
            "(:endDate IS NULL OR g.grnDate <= :endDate) AND " +
            "(:grnNo IS NULL OR lower(g.grnNo) LIKE lower(concat('%', :grnNo, '%'))) AND " +
            "(:invoiceNo IS NULL OR lower(g.invoiceNo) LIKE lower(concat('%', :invoiceNo, '%'))) " +
            "ORDER BY g.grnDate DESC, g.grnId DESC")
    List<Grn> findByFilters(
            @Param("branchId") Long branchId,
            @Param("supplierId") Long supplierId,
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("grnNo") String grnNo,
            @Param("invoiceNo") String invoiceNo
    );

    long countByBranchIdAndGrnDate(Long branchId, LocalDate grnDate);

    Long countByBranchIdAndStatus(Long branchId, String status);

    Long countByStatus(String status);
}
