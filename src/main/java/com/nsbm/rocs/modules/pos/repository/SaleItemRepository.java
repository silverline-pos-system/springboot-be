package com.nsbm.rocs.modules.pos.repository;

import com.nsbm.rocs.entity.pos.SaleItem;
import com.nsbm.rocs.modules.pos.dto.sale.ProductSalesHistoryDTO;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PURPOSE: Interface for sale_items operations
 */
@Repository
public interface SaleItemRepository extends JpaRepository<@NonNull SaleItem, @NonNull Long>, SaleItemRepositoryCustom {

    /**
     * Get all items for a sale
     * @param saleId - Sale ID
     * @return List of sale items
     */
    List<SaleItem> findBySaleId(Long saleId);

    /**
     * Delete items by sale ID
     * @param saleId - Sale ID
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySaleId(Long saleId);

    @Query("SELECT new com.nsbm.rocs.modules.pos.dto.sale.ProductSalesHistoryDTO(FUNCTION('DATE', s.saleDate), SUM(si.qty), SUM(si.total)) " +
           "FROM SaleItem si JOIN Sale s ON si.saleId = s.saleId " +
           "WHERE si.productId = :productId AND s.saleDate BETWEEN :startDate AND :endDate " +
           "AND s.paymentStatus = 'PAID' " +
           "GROUP BY FUNCTION('DATE', s.saleDate) " +
           "ORDER BY FUNCTION('DATE', s.saleDate)")
    List<ProductSalesHistoryDTO> findDailySalesByProduct(@Param("productId") Long productId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
}
