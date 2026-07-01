package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.dto.sale.SaleSummaryDTO;
import com.silverline.erp.module.pos.dto.sale.ProductSalesHistoryDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SaleQueryService {
    Map<String, Object> getLastInvoiceInfo();
    SaleResponse getSaleById(Long id);
    List<SaleResponse> getSales(String status);
    List<SaleResponse> getReturnableSales(Long branchId, Integer days);
    SaleResponse getSaleByInvoiceNo(String invoiceNo);
    List<SaleSummaryDTO> getSaleSummaries(Long branchId, String status, String startDateStr, String endDateStr);
    List<SaleSummaryDTO> getHeldBills(Long branchId);
    List<ProductSalesHistoryDTO> getProductSalesHistory(Long productId, LocalDate from, LocalDate to);
    SaleResponse mapToResponse(com.silverline.erp.domain.pos.Sale sale, List<com.silverline.erp.domain.pos.SaleItem> items, List<com.silverline.erp.domain.pos.Payment> payments);
}
