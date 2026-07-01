package com.silverline.erp.module.finance.controller;

import com.silverline.erp.module.finance.dto.ChartOfAccountDTO;
import com.silverline.erp.module.finance.dto.JournalEntryDTO;
import com.silverline.erp.module.finance.dto.JournalEntryRequest;
import com.silverline.erp.module.finance.dto.ProfitLossDTO;
import com.silverline.erp.module.finance.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Accounting endpoints.
 * Base path: /api/inventory/accounting
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;

    // ===== CHART OF ACCOUNTS =====

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<List<ChartOfAccountDTO>> getChartOfAccounts() {
        log.info("Fetching chart of accounts");
        List<ChartOfAccountDTO> accounts = accountingService.getChartOfAccounts();
        return ResponseEntity.ok(accounts);
    }

    // ===== JOURNAL ENTRIES =====

    @GetMapping("/journal-entries")
    public ResponseEntity<List<JournalEntryDTO>> getJournalEntries(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching journal entries with limit: {}", limit);
        List<JournalEntryDTO> entries = accountingService.getJournalEntries(limit);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntryDTO> createJournalEntry(
            @RequestBody JournalEntryRequest request) {
        log.info("Creating journal entry: {}", request.getDescription());
        JournalEntryDTO entry = accountingService.createJournalEntry(request);
        return ResponseEntity.ok(entry);
    }

    // ===== PROFIT & LOSS =====

    @GetMapping("/profit-loss")
    public ResponseEntity<ProfitLossDTO> getProfitAndLoss(
            @RequestParam(defaultValue = "monthly") String period) {
        log.info("Fetching P&L for period: {}", period);
        ProfitLossDTO plReport = accountingService.getProfitAndLoss(period);
        return ResponseEntity.ok(plReport);
    }
}


