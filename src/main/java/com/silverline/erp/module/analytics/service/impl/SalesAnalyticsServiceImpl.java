package com.silverline.erp.module.analytics.service.impl;

import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.analytics.dto.*;
import com.silverline.erp.module.analytics.service.SalesAnalyticsService;
import com.silverline.erp.module.pos.service.SaleQueryService;
import com.silverline.erp.module.admin.service.UserService;
import com.silverline.erp.module.admin.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsServiceImpl implements SalesAnalyticsService {

    private final SaleQueryService saleQueryService;
    private final UserService userService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<SalesDataDTO> getSalesData(String period, Long branchId) {
        int days = switch (period.toLowerCase()) {
            case "daily" -> 1;
            case "monthly" -> 30;
            default -> 7;
        };

        List<SalesDataDTO> salesData = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("EEE");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            BigDecimal dailySales;
            Long transactions;
            if (branchId != null) {
                dailySales = saleQueryService.sumNetTotalByBranchAndDateRange(branchId, dayStart, dayEnd);
                transactions = saleQueryService.countByBranchAndDateRange(branchId, dayStart, dayEnd);
            } else {
                dailySales = saleQueryService.sumNetTotalByDateRange(dayStart, dayEnd);
                transactions = saleQueryService.countByDateRange(dayStart, dayEnd);
            }

            salesData.add(SalesDataDTO.builder()
                    .label(date.format(labelFormatter))
                    .value(dailySales != null ? dailySales : BigDecimal.ZERO)
                    .transactions(transactions != null ? transactions.intValue() : 0)
                    .build());
        }

        return salesData;
    }

    @Override
    public List<TopSellingProductDTO> getTopSellingProducts(int limit, Long branchId) {
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime weekEnd = LocalDateTime.now();

        List<Object[]> results;
        if (branchId != null) {
            results = saleQueryService.findTopSellingProductsByBranch(branchId, weekStart, weekEnd, limit);
        } else {
            results = saleQueryService.findTopSellingProducts(weekStart, weekEnd, limit);
        }

        return results.stream()
                .map(row -> TopSellingProductDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .sku((String) row[2])
                        .units(((Number) row[3]).intValue())
                        .revenue(formatCurrency((BigDecimal) row[4]))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public SalesAnalyticsDTO getSalesAnalytics(String period, Long branchId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);

        int days = switch (period.toLowerCase()) {
            case "daily" -> 1;
            case "monthly" -> 30;
            default -> 7;
        };
        LocalDateTime periodStart = LocalDate.now().minusDays(days - 1).atStartOfDay();

        BigDecimal todaySales;
        BigDecimal yesterdaySales;
        Long todayTransactions;
        Long yesterdayTransactions;
        Long customersServed;

        if (branchId != null) {
            todaySales = saleQueryService.sumNetTotalByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdaySales = saleQueryService.sumNetTotalByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            todayTransactions = saleQueryService.countByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdayTransactions = saleQueryService.countByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            customersServed = saleQueryService.countDistinctCustomers(todayStart, todayEnd, branchId);
        } else {
            todaySales = saleQueryService.sumNetTotalByDateRange(todayStart, todayEnd);
            yesterdaySales = saleQueryService.sumNetTotalByDateRange(yesterdayStart, yesterdayEnd);
            todayTransactions = saleQueryService.countByDateRange(todayStart, todayEnd);
            yesterdayTransactions = saleQueryService.countByDateRange(yesterdayStart, yesterdayEnd);
            customersServed = saleQueryService.countDistinctCustomers(todayStart, todayEnd, null);
        }

        todaySales = todaySales != null ? todaySales : BigDecimal.ZERO;
        yesterdaySales = yesterdaySales != null ? yesterdaySales : BigDecimal.ZERO;
        int txnCount = todayTransactions != null ? todayTransactions.intValue() : 0;

        BigDecimal avgTransaction = txnCount > 0
                ? todaySales.divide(BigDecimal.valueOf(txnCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double growth = 0.0;
        if (yesterdaySales.compareTo(BigDecimal.ZERO) > 0) {
            growth = todaySales.subtract(yesterdaySales)
                    .divide(yesterdaySales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        List<PaymentBreakdownDTO> paymentBreakdown = getPaymentBreakdown(todayStart, todayEnd, branchId);
        List<HourlySalesDTO> hourlySales = getHourlySales(LocalDateTime.now(), branchId);
        List<RecentTransactionDTO> recentTransactions = getRecentTransactions(15, branchId);
        List<TopSellingProductDTO> topProducts = getTopSellingProducts(5, branchId);
        List<SalesDataDTO> dailyTrend = getSalesData(period, branchId);

        return SalesAnalyticsDTO.builder()
                .todaySales(todaySales)
                .yesterdaySales(yesterdaySales)
                .weeklyAverage(calculateWeeklyAverage())
                .todayTransactions(txnCount)
                .yesterdayTransactions(yesterdayTransactions != null ? yesterdayTransactions.intValue() : 0)
                .avgTransactionValue(avgTransaction)
                .customersServed(customersServed != null ? customersServed.intValue() : 0)
                .growthPercentage(growth)
                .paymentBreakdown(paymentBreakdown)
                .hourlySales(hourlySales)
                .recentTransactions(recentTransactions)
                .topProducts(topProducts)
                .dailyTrend(dailyTrend)
                .build();
    }

    @Override
    public List<SalesReportDTO> getSalesReports(String startDateStr, String endDateStr, Long branchId) {
        LocalDate startDate = startDateStr != null
                ? LocalDate.parse(startDateStr)
                : LocalDate.now().minusDays(7);
        LocalDate endDate = endDateStr != null
                ? LocalDate.parse(endDateStr)
                : LocalDate.now();

        List<SalesReportDTO> reports = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            BigDecimal revenue;
            BigDecimal grossTotal;
            Long invoiceCount;

            if (branchId != null) {
                revenue = saleQueryService.sumNetTotalByBranchAndDateRange(branchId, dayStart, dayEnd);
                grossTotal = saleQueryService.sumGrossTotalByDateRange(dayStart, dayEnd, branchId);
                invoiceCount = saleQueryService.countByBranchAndDateRange(branchId, dayStart, dayEnd);
            } else {
                revenue = saleQueryService.sumNetTotalByDateRange(dayStart, dayEnd);
                grossTotal = saleQueryService.sumGrossTotalByDateRange(dayStart, dayEnd, null);
                invoiceCount = saleQueryService.countByDateRange(dayStart, dayEnd);
            }

            revenue = revenue != null ? revenue : BigDecimal.ZERO;
            grossTotal = grossTotal != null ? grossTotal : BigDecimal.ZERO;
            int invoices = invoiceCount != null ? invoiceCount.intValue() : 0;

            BigDecimal cashSales = saleQueryService.sumPaymentByTypeAndDateRange(dayStart, dayEnd, "CASH", branchId);
            BigDecimal cardSales = saleQueryService.sumPaymentByTypeAndDateRange(dayStart, dayEnd, "CARD", branchId);
            BigDecimal qrSales = saleQueryService.sumPaymentByTypeAndDateRange(dayStart, dayEnd, "QR", branchId);

            cashSales = cashSales != null ? cashSales : BigDecimal.ZERO;
            cardSales = cardSales != null ? cardSales : BigDecimal.ZERO;
            qrSales = qrSales != null ? qrSales : BigDecimal.ZERO;

            BigDecimal returns = saleQueryService.sumReturnTotalByDateRange(dayStart, dayEnd, branchId);
            returns = returns != null ? returns : BigDecimal.ZERO;

            BigDecimal cost = revenue.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = revenue.subtract(cost);

            BigDecimal avgBasket = invoices > 0
                    ? revenue.divide(BigDecimal.valueOf(invoices), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            double profitMargin = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            reports.add(SalesReportDTO.builder()
                    .date(date.format(DATE_FORMATTER))
                    .dayName(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .invoices(invoices)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(profit)
                    .cashSales(cashSales)
                    .cardSales(cardSales)
                    .qrSales(qrSales)
                    .returns(returns)
                    .avgBasket(avgBasket)
                    .profitMargin(profitMargin)
                    .build());
        }

        return reports;
    }

    @Override
    public List<TerminalSalesDTO> getSalesByTerminal(String startDateStr, String endDateStr, Long branchId) {
        return java.util.Collections.emptyList();
    }

    private BigDecimal calculateWeeklyAverage() {
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime weekEnd = LocalDateTime.now();
        BigDecimal weekTotal = saleQueryService.sumNetTotalByDateRange(weekStart, weekEnd);
        return weekTotal != null ? weekTotal.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private List<PaymentBreakdownDTO> getPaymentBreakdown(LocalDateTime startDate, LocalDateTime endDate, Long branchId) {
        List<Object[]> results = saleQueryService.findPaymentBreakdownByDateRange(startDate, endDate, branchId);
        BigDecimal total = results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return results.stream()
                .map(row -> {
                    String method = (String) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    int count = ((Number) row[2]).intValue();
                    double percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;

                    return PaymentBreakdownDTO.builder()
                            .method(method != null ? method : "OTHER")
                            .amount(amount)
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<HourlySalesDTO> getHourlySales(LocalDateTime targetDate, Long branchId) {
        List<Object[]> results = saleQueryService.findHourlySales(targetDate, branchId);

        Map<Integer, Object[]> hourlyMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row
                ));

        List<HourlySalesDTO> hourlyList = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Object[] data = hourlyMap.get(hour);
            String hourLabel = String.format("%02d:00", hour);

            if (data != null) {
                hourlyList.add(HourlySalesDTO.builder()
                        .hour(hourLabel)
                        .sales((BigDecimal) data[1])
                        .transactions(((Number) data[2]).intValue())
                        .build());
            } else {
                hourlyList.add(HourlySalesDTO.builder()
                        .hour(hourLabel)
                        .sales(BigDecimal.ZERO)
                        .transactions(0)
                        .build());
            }
        }
        return hourlyList;
    }

    private List<RecentTransactionDTO> getRecentTransactions(int limit, Long branchId) {
        List<Sale> recentSales;
        if (branchId != null) {
            recentSales = saleQueryService.findRecentSalesByBranch(branchId, limit);
        } else {
            recentSales = saleQueryService.findRecentSales(limit);
        }

        Map<Long, String> userNames = userService.getAllUsers().stream()
                .collect(Collectors.toMap(UserDTO::getUserId, UserDTO::getFullName, (a, b) -> a));

        return recentSales.stream()
                .map(sale -> {
                    List<SaleItem> items = saleQueryService.findSaleItemsBySaleId(sale.getSaleId());
                    int itemCount = items != null ? items.size() : 0;

                    List<Payment> payments = saleQueryService.findPaymentsBySaleId(sale.getSaleId());
                    String paymentMethod = payments != null && !payments.isEmpty()
                            ? payments.get(0).getPaymentType()
                            : "CASH";

                    return RecentTransactionDTO.builder()
                            .saleId(sale.getSaleId())
                            .invoiceNo(sale.getInvoiceNo())
                            .cashier(userNames.getOrDefault(sale.getCashierId(), "Unknown"))
                            .itemCount(itemCount)
                            .amount(sale.getNetTotal())
                            .paymentMethod(paymentMethod)
                            .type("SALE")
                            .time(sale.getSaleDate().format(TIME_FORMATTER))
                            .date(sale.getSaleDate().format(DATE_FORMATTER))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "LKR " + String.format("%,.0f", amount);
    }
}
