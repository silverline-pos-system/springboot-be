package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.pos.CashShift;
import java.util.Optional;
import java.util.List;

/**
 * PURPOSE: Interface for all shift-related database operations
 * NOTE: Terminal-based operations REMOVED â€” shifts tied to branches only
 */
public interface ShiftRepository {

    /**
     * Save a new shift to database
     * @param shift - CashShift entity to save
     * @return Generated shift_id
     */
    Long save(CashShift shift);

    /**
     * Update existing shift (used when closing)
     * @param shift - CashShift entity with updated values
     */
    void update(CashShift shift);

    /**
     * Find shift by ID
     * @param shiftId - Primary key
     * @return Optional<CashShifts> - Present if found, empty if not
     */
    Optional<CashShift> findById(Long shiftId);

    /**
     * Find the currently open shift for a cashier
     * @param cashierId - User ID of cashier
     * @return Optional<CashShift>
     */
    Optional<CashShift> findOpenShiftByCashierId(Long cashierId);

    /**
     * Check if cashier has an open shift
     * @param cashierId - User ID of cashier
     * @return true if open shift exists, false otherwise
     */
    boolean hasOpenShift(Long cashierId);

    /**
     * Get shift with transaction statistics
     * @param shiftId - Primary key
     * @return Optional<CashShift> with transaction stats
     */
    Optional<CashShift> findByIdWithStats(Long shiftId);

    /**
     * Get all shifts for a specific branch
     * @param branchId - Branch ID
     * @param limit - Max number of records
     * @return List of shifts
     */
    List<CashShift> findByBranchId(Long branchId, int limit);

    /**
     * Get all shifts for a specific cashier
     * @param cashierId - User ID
     * @param limit - Max number of records
     * @return List of shifts
     */
    List<CashShift> findByCashierId(Long cashierId, int limit);

    /**
     * Find active shift for a cashier
     * @param cashierId - User ID
     * @return Optional<CashShift>
     */
    Optional<CashShift> findActiveShiftByCashier(Long cashierId);

    // NOTE: findOpenShiftByTerminalId REMOVED â€” terminal concept eliminated

    /**
     * Find open shift for a specific branch
     * @param branchId - Branch ID
     * @return Optional<CashShift>
     */
    Optional<CashShift> findOpenShiftByBranchId(Long branchId);
}
