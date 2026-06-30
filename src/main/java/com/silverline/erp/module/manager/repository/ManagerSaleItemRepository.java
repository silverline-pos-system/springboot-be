package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.pos.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerSaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query(value = """
        SELECT si.product_id, p.name, p.sku, 
               SUM(si.qty) as total_qty, 
               SUM(si.total) as total_revenue
        FROM sale_items si
        JOIN products p ON si.product_id = p.product_id
        JOIN sales s ON si.sale_id = s.sale_id
        WHERE s.sale_date BETWEEN :startDate AND :endDate
        GROUP BY si.product_id, p.name, p.sku
        ORDER BY total_qty DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("startDate") java.time.LocalDateTime startDate,
                                          @Param("endDate") java.time.LocalDateTime endDate,
                                          @Param("limit") int limit);

    @Query(value = """
        SELECT si.product_id, p.name, p.sku, 
               SUM(si.qty) as total_qty, 
               SUM(si.total) as total_revenue
        FROM sale_items si
        JOIN products p ON si.product_id = p.product_id
        JOIN sales s ON si.sale_id = s.sale_id
        WHERE s.branch_id = :branchId AND s.sale_date BETWEEN :startDate AND :endDate
        GROUP BY si.product_id, p.name, p.sku
        ORDER BY total_qty DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSellingProductsByBranch(@Param("branchId") Long branchId,
                                                   @Param("startDate") java.time.LocalDateTime startDate,
                                                   @Param("endDate") java.time.LocalDateTime endDate,
                                                   @Param("limit") int limit);
}


