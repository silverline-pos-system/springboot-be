package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.module.admin.repository.SaleRepository;
import com.silverline.erp.module.admin.service.AdminSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminSaleServiceImpl implements AdminSaleService {

    private final SaleRepository saleRepository;

    /**
     * Sum netTotal for sales in the given date range and optional branch.
     * If start or end are null, caller may pass LocalDateTime.MIN / MAX. If branchId is null, repository should handle it.
     *
     * @param branchId optional branch id filter
     * @param start inclusive start datetime
     * @param end inclusive end datetime
     * @return sum of netTotal (BigDecimal.ZERO when none)
     */
    @Override
    public BigDecimal getSumNetTotal(Long branchId, LocalDateTime start, LocalDateTime end) {
        // Use safe bounds instead of LocalDateTime.MIN / LocalDateTime.MAX which produce years
        // that can't be converted to SQL/TIMESTAMP (causes "Invalid value for Year" errors).
        if (start == null) start = LocalDateTime.of(1970, 1, 1, 0, 0);
        if (end == null) end = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        List<Sale> sales = saleRepository.findByDateRange(branchId, start, end);
        if (sales == null || sales.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return sales.stream()
                .map(Sale::getNetTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Sum netTotal for all branches for all time.
     * Delegates to getSumNetTotal with null branch and full date range.
     *
     * @return total netTotal across all branches (BigDecimal.ZERO if none)
     */
    @Override
    public BigDecimal getTotalNetAllTime() {
        return getSumNetTotal(null, null, null);
    }
}

