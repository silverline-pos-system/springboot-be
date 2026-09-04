package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.pos.dto.CartPricing;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;

public interface PosSaleService {
    SaleResponse createSale(CreateSaleRequest request, Long branchId, Long cashierId, Long shiftId);

    void updateSaleStatus(Long saleId, String status);

    /** Live cart pricing: batch prices plus promotions, without persisting anything. */
    CartPricing.Response priceCart(CartPricing.Request request);
}
