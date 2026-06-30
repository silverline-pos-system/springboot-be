package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.inventory.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("managerDispatchRepository")
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    Long countByBranchIdAndStatus(Long branchId, String status);

    Long countByStatus(String status);

        @Query(value = "SELECT g.dispatch_id, g.dispatch_no, g.dispatch_date, g.status, g.total_amount, s.name as supplier_name, COUNT(gi.dispatch_line_id) as item_count " +
            "FROM item_dispatches g " +
            "LEFT JOIN suppliers s ON g.supplier_id = s.supplier_id " +
            "LEFT JOIN item_dispatch_lines gi ON g.dispatch_id = gi.dispatch_id " +
            "WHERE g.status = 'PENDING' " +
            "GROUP BY g.dispatch_id, g.dispatch_no, g.dispatch_date, g.status, g.total_amount, s.name",
            nativeQuery = true)
    List<Object[]> findAllPendingDispatchesWithDetails();
}


