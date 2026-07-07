package com.silverline.erp.module.finance.controller;

import com.silverline.erp.module.finance.dto.ChartOfAccountDTO;
import com.silverline.erp.module.finance.dto.JournalEntryDTO;
import com.silverline.erp.module.finance.dto.JournalEntryRequest;
import com.silverline.erp.module.finance.dto.ProfitLossDTO;
import com.silverline.erp.module.finance.service.AccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/finance/accounting", "/api/inventory/accounting"})
@RequiredArgsConstructor
@Tag(name = "General Ledger & Accounts", description = "APIs for querying chart of accounts, creating double-entry journal logs, and compiling profit & loss reports")
public class AccountingController {

    private final AccountingService accountingService;

    // ===== CHART OF ACCOUNTS =====

    @Operation(summary = "Get chart of accounts", description = "Retrieves the list of ledger accounts (assets, liabilities, equity, revenues, expenses)")
    @ApiResponse(responseCode = "200", description = "Chart of accounts fetched successfully")
    @GetMapping("/chart-of-accounts")
    public ResponseEntity<List<ChartOfAccountDTO>> getChartOfAccounts() {
        log.info("Fetching chart of accounts");
        List<ChartOfAccountDTO> accounts = accountingService.getChartOfAccounts();
        return ResponseEntity.ok(accounts);
    }

    // ===== JOURNAL ENTRIES =====

    @Operation(summary = "Get latest journal entries", description = "Retrieves a list of recent journal transactions up to the specified limit (defaults to 10)")
    @ApiResponse(responseCode = "200", description = "Journal entries retrieved successfully")
    @GetMapping("/journal-entries")
    public ResponseEntity<List<JournalEntryDTO>> getJournalEntries(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching journal entries with limit: {}", limit);
        List<JournalEntryDTO> entries = accountingService.getJournalEntries(limit);
        return ResponseEntity.ok(entries);
    }

    @Operation(summary = "Create journal entry", description = "Posts a manual double-entry journal entry, validating balanced debit and credit items")
    @ApiResponse(responseCode = "200", description = "Journal entry posted successfully")
    @ApiResponse(responseCode = "400", description = "Unbalanced debit/credit amounts or invalid account IDs")
    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntryDTO> createJournalEntry(
            @RequestBody JournalEntryRequest request) {
        log.info("Creating journal entry: {}", request.getDescription());
        JournalEntryDTO entry = accountingService.createJournalEntry(request);
        return ResponseEntity.ok(entry);
    }

    // ===== PROFIT & LOSS =====

    @Operation(summary = "Get Profit & Loss statement", description = "Compiles total revenues, costs of goods sold, and expenses for a period (e.g. monthly, quarterly, yearly)")
    @ApiResponse(responseCode = "200", description = "Profit & Loss statement generated successfully")
    @GetMapping("/profit-loss")
    public ResponseEntity<ProfitLossDTO> getProfitAndLoss(
            @RequestParam(defaultValue = "monthly") String period) {
        log.info("Fetching P&L for period: {}", period);
        ProfitLossDTO plReport = accountingService.getProfitAndLoss(period);
        return ResponseEntity.ok(plReport);
    }
}
