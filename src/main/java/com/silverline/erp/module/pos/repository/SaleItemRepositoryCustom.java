package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.module.pos.dto.sale.SaleItemResponse;

import java.util.List;

public interface SaleItemRepositoryCustom {
    void saveBatch(List<SaleItem> saleItems);
    List<SaleItemResponse> findBySaleIdWithProductDetails(Long saleId);
}

