package com.silverline.erp.module.admin.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdminSaleService {
    BigDecimal getSumNetTotal(Long branchId, LocalDateTime start, LocalDateTime end);

    BigDecimal getTotalNetAllTime();
}

