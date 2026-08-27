package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.Sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PURPOSE: Interface for all Sales-related database operations
 */
public interface SaleRepository {

    /**
     * Save a new Sales
     *
     * @param Sale - Sales entity
     * @return Generated Sales_id
     */
    Long save(Sale Sale);

    /**
     * Find Sales by ID
     *
     * @param SalesId - Primary key
     * @return Optional<Sales>
     */
    Optional<Sale> findById(Long SalesId);

    /**
     * Find Sales by invoice number
     *
     * @param invoiceNo - Invoice number
     * @return Optional<Sales>
     */
    Optional<Sale> findByInvoiceNo(String invoiceNo);

    /**
     * Find a sale previously created with the given idempotency key, if any.
     * Used to make a retried checkout return the original sale instead of duplicating it.
     */
    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    /**
     * Get all Saless for a shift
     *
     * @param shiftId - Shift ID
     * @return List of Saless
     */
    List<Sale> findByShiftId(Long shiftId);

    /**
     * Get Saless by date range
     *
     * @param branchId  - Branch ID
     * @param startDate - Start date
     * @param endDate   - End date
     * @return List of Saless
     */
    List<Sale> findByDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get total Saless count for a shift
     *
     * @param shiftId - Shift ID
     * @return Count
     */
    int countByShiftId(Long shiftId);

    /**
     * Get all sales
     *
     * @return List of sales
     */
    List<Sale> findAll();

    /**
     * Find by payment status
     *
     * @param status - Status (PAID, etc)
     * @return List of sales
     */
    List<Sale> findByPaymentStatus(String status);

    /**
     * Get total net sales amount for a shift
     *
     * @param shiftId - Shift ID
     * @return Total net sales
     */
    BigDecimal sumNetTotalByShiftId(Long shiftId);

    /**
     * Get the last invoice number
     *
     * @return Last invoice number or null if none exists
     */
    String findLastInvoiceNo();

    /**
     * Get the last invoice number for today
     *
     * @param datePrefix - Date prefix in format INV-YYYYMMDD
     * @return Last invoice number for today or null if none exists
     */
    String findLastInvoiceNoByDatePrefix(String datePrefix);

    /**
     * Atomically reserve the next sequence number for a given invoice date prefix.
     * Concurrency-safe: uses an upsert-with-increment so two simultaneous sales never get the same number.
     *
     * @param datePrefix - Date prefix in format INV-YYYYMMDD
     * @return the next sequence (1 for the first sale of the day)
     */
    int nextInvoiceSequence(String datePrefix);

    /**
     * Get total net sales for today (all branches)
     */
    BigDecimal sumNetTotalForToday();

    /**
     * Get total net sales all time (all branches)
     */
    BigDecimal sumNetTotalAllTime();

    /**
     * Get top branches by sales volume
     * Returns List of [BranchId, BranchName, TotalSales]
     */
    List<Object[]> findTopBranches(int limit);

    /**
     * Get daily sales for last N days
     * Returns List of [Date, TotalSales]
     */
    List<Object[]> findLastNDaysSales(int days);
}
