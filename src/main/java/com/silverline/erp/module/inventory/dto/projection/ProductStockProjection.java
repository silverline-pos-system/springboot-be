package com.silverline.erp.module.inventory.dto.projection;

import java.math.BigDecimal;

public interface ProductStockProjection {
    Long getProductId();
    String getName();
    BigDecimal getReorderLevel();
}
