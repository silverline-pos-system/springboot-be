package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.pos.dto.sale.ProductSalesHistoryDTO;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.dto.sale.SaleSummaryDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SaleQueryService {
    Map<String, Object> getLastInvoiceInfo();
    SaleResponse getSaleById(Long id);
    List<SaleResponse> getSales(String status);
    List<SaleResponse> getReturnableSales(Long branchId, Integer days);
    SaleResponse getSaleByInvoiceNo(String invoiceNo);
    Page<SaleSummaryDTO> getSaleSummaries(Long branchId, String status, String startDateStr, String endDateStr, Pageable pageable);
    List<SaleSummaryDTO> getHeldBills(Long branchId);
    List<ProductSalesHistoryDTO> getProductSalesHistory(Long productId, LocalDate from, LocalDate to);
    SaleResponse mapToResponse(com.silverline.erp.domain.pos.Sale sale, List<com.silverline.erp.domain.pos.SaleItem> items, List<com.silverline.erp.domain.pos.Payment> payments);

    // Methods needed by analytics
    java.math.BigDecimal sumNetTotalByBranchAndDateRange(Long branchId, LocalDateTime start, LocalDateTime end);
    Long countByBranchAndDateRange(Long branchId, LocalDateTime start, LocalDateTime end);
    java.math.BigDecimal sumNetTotalByDateRange(LocalDateTime start, LocalDateTime end);
    Long countByDateRange(LocalDateTime start, LocalDateTime end);
    List<Object[]> findTopSellingProductsByBranch(Long branchId, LocalDateTime start, LocalDateTime end, int limit);
    List<Object[]> findTopSellingProducts(LocalDateTime start, LocalDateTime end, int limit);
    Long countDistinctCustomers(LocalDateTime start, LocalDateTime end, Long branchId);
    java.math.BigDecimal sumGrossTotalByDateRange(LocalDateTime start, LocalDateTime end, Long branchId);
    java.math.BigDecimal sumPaymentByTypeAndDateRange(LocalDateTime start, LocalDateTime end, String type, Long branchId);
    java.math.BigDecimal sumReturnTotalByDateRange(LocalDateTime start, LocalDateTime end, Long branchId);
    List<Object[]> findPaymentBreakdownByDateRange(LocalDateTime start, LocalDateTime end, Long branchId);
    List<Object[]> findHourlySales(LocalDateTime targetDate, Long branchId);
    List<com.silverline.erp.domain.pos.Sale> findRecentSalesByBranch(Long branchId, int limit);
    List<com.silverline.erp.domain.pos.Sale> findRecentSales(int limit);
    List<com.silverline.erp.domain.pos.SaleItem> findSaleItemsBySaleId(Long saleId);
    List<com.silverline.erp.domain.pos.Payment> findPaymentsBySaleId(Long saleId);
    List<com.silverline.erp.domain.pos.Sale> findTop10ByCustomerIdOrderBySaleDateDesc(Long customerId);
}
