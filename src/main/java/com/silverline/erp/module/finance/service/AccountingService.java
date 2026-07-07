package com.silverline.erp.module.finance.service;

import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.module.analytics.dto.SalesReportDTO;
import com.silverline.erp.module.finance.dto.ChartOfAccountDTO;
import com.silverline.erp.module.finance.dto.JournalEntryDTO;
import com.silverline.erp.module.finance.dto.JournalEntryRequest;
import com.silverline.erp.module.finance.dto.ProfitLossDTO;
import com.silverline.erp.module.manager.repository.ManagerSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final ManagerSaleRepository saleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ===== CHART OF ACCOUNTS =====

    public List<ChartOfAccountDTO> getChartOfAccounts() {
        // Return predefined chart of accounts for retail business
        List<ChartOfAccountDTO> accounts = new ArrayList<>();

        // Assets
        accounts.add(ChartOfAccountDTO.builder().code("1000").name("Cash").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1010").name("Bank Account").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1100").name("Accounts Receivable").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1200").name("Inventory").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1300").name("Prepaid Expenses").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1500").name("Equipment").type("Asset").build());
        accounts.add(ChartOfAccountDTO.builder().code("1510").name("Accumulated Depreciation").type("Asset").build());

        // Liabilities
        accounts.add(ChartOfAccountDTO.builder().code("2000").name("Accounts Payable").type("Liability").build());
        accounts.add(ChartOfAccountDTO.builder().code("2100").name("Accrued Expenses").type("Liability").build());
        accounts.add(ChartOfAccountDTO.builder().code("2200").name("Short-term Loans").type("Liability").build());
        accounts.add(ChartOfAccountDTO.builder().code("2300").name("Long-term Loans").type("Liability").build());

        // Equity
        accounts.add(ChartOfAccountDTO.builder().code("3000").name("Owner's Capital").type("Equity").build());
        accounts.add(ChartOfAccountDTO.builder().code("3100").name("Retained Earnings").type("Equity").build());

        // Revenue
        accounts.add(ChartOfAccountDTO.builder().code("4000").name("Sales Revenue").type("Revenue").build());
        accounts.add(ChartOfAccountDTO.builder().code("4100").name("Service Revenue").type("Revenue").build());
        accounts.add(ChartOfAccountDTO.builder().code("4200").name("Other Income").type("Revenue").build());

        // Cost of Goods Sold
        accounts.add(ChartOfAccountDTO.builder().code("5000").name("Cost of Goods Sold").type("COGS").build());
        accounts.add(ChartOfAccountDTO.builder().code("5100").name("Purchase Discounts").type("COGS").build());

        // Expenses
        accounts.add(ChartOfAccountDTO.builder().code("6000").name("Salaries & Wages").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6100").name("Rent Expense").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6200").name("Utilities").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6300").name("Insurance").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6400").name("Marketing & Advertising").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6500").name("Office Supplies").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6600").name("Depreciation Expense").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6700").name("Bank Charges").type("Expense").build());
        accounts.add(ChartOfAccountDTO.builder().code("6800").name("Miscellaneous Expense").type("Expense").build());

        return accounts;
    }

    // ===== JOURNAL ENTRIES =====

    public List<JournalEntryDTO> getJournalEntries(int limit) {
        // In a full implementation, this would fetch from a journal_entries table
        // For now, return sample entries based on sales data
        List<JournalEntryDTO> entries = new ArrayList<>();

        LocalDate today = LocalDate.now();

        // Create sample journal entries
        entries.add(JournalEntryDTO.builder()
                .entryId(1L)
                .date(today.format(DATE_FORMATTER))
                .description("Cash sales for the day")
                .lines(List.of(
                        JournalEntryDTO.JournalLineDTO.builder()
                                .account("1000 - Cash")
                                .dr(new BigDecimal("50000"))
                                .cr(BigDecimal.ZERO)
                                .build(),
                        JournalEntryDTO.JournalLineDTO.builder()
                                .account("4000 - Sales Revenue")
                                .dr(BigDecimal.ZERO)
                                .cr(new BigDecimal("50000"))
                                .build()
                ))
                .createdAt(today.atTime(LocalTime.of(18, 0)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .createdBy("System")
                .build());

        entries.add(JournalEntryDTO.builder()
                .entryId(2L)
                .date(today.minusDays(1).format(DATE_FORMATTER))
                .description("Rent payment")
                .lines(List.of(
                        JournalEntryDTO.JournalLineDTO.builder()
                                .account("6100 - Rent Expense")
                                .dr(new BigDecimal("75000"))
                                .cr(BigDecimal.ZERO)
                                .build(),
                        JournalEntryDTO.JournalLineDTO.builder()
                                .account("1010 - Bank Account")
                                .dr(BigDecimal.ZERO)
                                .cr(new BigDecimal("75000"))
                                .build()
                ))
                .createdAt(today.minusDays(1).atTime(LocalTime.of(10, 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .createdBy("Manager")
                .build());

        return entries.stream().limit(limit).collect(Collectors.toList());
    }

    public JournalEntryDTO createJournalEntry(JournalEntryRequest request) {
        // In a full implementation, this would save to a journal_entries table
        log.info("Creating journal entry: {}", request.getDescription());

        List<JournalEntryDTO.JournalLineDTO> lines = request.getLines().stream()
                .map(line -> JournalEntryDTO.JournalLineDTO.builder()
                        .account(line.getAccount())
                        .dr(line.getDr() != null ? line.getDr() : BigDecimal.ZERO)
                        .cr(line.getCr() != null ? line.getCr() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return JournalEntryDTO.builder()
                .entryId(System.currentTimeMillis())
                .date(request.getDate())
                .description(request.getDescription())
                .lines(lines)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .createdBy("Manager")
                .build();
    }

    // ===== PROFIT & LOSS =====

    public ProfitLossDTO getProfitAndLoss(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;
        String periodLabel;

        switch (period.toLowerCase()) {
            case "weekly":
                startDate = endDate.minusDays(7);
                periodLabel = "Last 7 Days";
                break;
            case "yearly":
                startDate = endDate.minusYears(1);
                periodLabel = "Last 12 Months";
                break;
            case "monthly":
            default:
                startDate = endDate.minusDays(30);
                periodLabel = "Last 30 Days";
                break;
        }

        // Calculate revenue from sales
        BigDecimal revenue = saleRepository.sumNetTotalByDateRange(startDate, endDate);
        if (revenue == null) revenue = BigDecimal.ZERO;

        // Estimate COGS as ~60% of revenue (typical retail margin)
        BigDecimal cogs = revenue.multiply(new BigDecimal("0.60"));
        BigDecimal grossProfit = revenue.subtract(cogs);

        // Sample expenses
        List<ProfitLossDTO.ExpenseDTO> expenses = new ArrayList<>();
        expenses.add(ProfitLossDTO.ExpenseDTO.builder().name("Salaries & Wages").amount(new BigDecimal("150000")).build());
        expenses.add(ProfitLossDTO.ExpenseDTO.builder().name("Rent").amount(new BigDecimal("75000")).build());
        expenses.add(ProfitLossDTO.ExpenseDTO.builder().name("Utilities").amount(new BigDecimal("25000")).build());
        expenses.add(ProfitLossDTO.ExpenseDTO.builder().name("Marketing").amount(new BigDecimal("15000")).build());
        expenses.add(ProfitLossDTO.ExpenseDTO.builder().name("Other Expenses").amount(new BigDecimal("10000")).build());

        BigDecimal totalExpenses = expenses.stream()
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        return ProfitLossDTO.builder()
                .period(periodLabel)
                .revenue(revenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .expenses(expenses)
                .netProfit(netProfit)
                .build();
    }

    // ===== SALES REPORTS =====

    public List<SalesReportDTO> getSalesReports(String from, String to) {
        List<SalesReportDTO> reports = new ArrayList<>();

        LocalDate startDate = from != null && !from.isEmpty()
                ? LocalDate.parse(from)
                : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null && !to.isEmpty()
                ? LocalDate.parse(to)
                : LocalDate.now();

        // Generate daily sales report
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            List<Sale> sales = saleRepository.findByDateRange(dayStart, dayEnd);

            if (!sales.isEmpty()) {
                BigDecimal revenue = sales.stream()
                        .map(s -> s.getNetTotal())
                        .filter(val -> val != null)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

                // Estimate profit as ~40% of revenue (after COGS)
                BigDecimal profit = revenue.multiply(new BigDecimal("0.40"));

                reports.add(SalesReportDTO.builder()
                        .date(date.format(DATE_FORMATTER))
                        .invoices(sales.size())
                        .revenue(revenue)
                        .profit(profit)
                        .build());
            }
        }

        return reports;
    }
}


