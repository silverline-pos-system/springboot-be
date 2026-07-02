package com.silverline.erp.module.pos.repository.impl;

import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShiftRepositoryImpl implements ShiftRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CashShift> shiftRowMapper = (rs, rowNum) -> {
        CashShift shift = new CashShift();

        shift.setShiftId(rs.getLong("shift_id"));
        shift.setShiftNo(rs.getString("shift_no"));
        shift.setBranchId(rs.getLong("branch_id"));
        shift.setCashierId(rs.getLong("cashier_id"));
        // NOTE: terminal_id REMOVED from mapping

        // Handle timestamps
        Timestamp openedTimestamp = rs.getTimestamp("opened_at");
        if (openedTimestamp != null) {
            shift.setOpenedAt(openedTimestamp.toLocalDateTime());
        }

        // Map approval info
        shift.setApprovedBy(rs.getLong("approved_by"));
        Timestamp approvedTimestamp = rs.getTimestamp("approved_at");
        if (approvedTimestamp != null) {
            shift.setApprovedAt(approvedTimestamp.toLocalDateTime());
        }

        Timestamp closedTimestamp = rs.getTimestamp("closed_at");
        if (closedTimestamp != null) {
            shift.setClosedAt(closedTimestamp.toLocalDateTime());
        }

        // Map money fields
        shift.setOpeningCash(rs.getBigDecimal("opening_cash"));
        shift.setClosingCash(rs.getBigDecimal("closing_cash"));
        shift.setExpectedCash(rs.getBigDecimal("expected_cash"));
        shift.setCashDifference(rs.getBigDecimal("cash_difference"));
        shift.setTotalSales(rs.getBigDecimal("total_sales"));
        shift.setTotalReturns(rs.getBigDecimal("total_returns"));

        // Map strings
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            shift.setStatus(CashShift.ShiftStatus.valueOf(statusStr));
        }
        shift.setNotes(rs.getString("notes"));

        return shift;
    };

    @Override
    public Long save(CashShift shift) {
        // NOTE: terminal_id REMOVED from INSERT
        // Uses PostgreSQL RETURNING clause instead of MySQL's LAST_INSERT_ID()
        String sql = "INSERT INTO cash_shifts " +
                "(shift_no, branch_id, cashier_id, opened_at, opening_cash, status, approved_by, approved_at, notes) " +
                "VALUES (?, ?, ?, NOW(), ?, ?, ?, ?, ?) " +
                "RETURNING shift_id";

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                shift.getShiftNo(),
                shift.getBranchId(),
                shift.getCashierId(),
                shift.getOpeningCash(),
                shift.getStatus() != null ? shift.getStatus().name() : "PENDING_APPROVAL",
                shift.getApprovedBy(),
                shift.getApprovedAt(),
                shift.getNotes()
        );
    }

    @Override
    public void update(CashShift shift) {
        String sql = "UPDATE cash_shifts SET " +
                "closed_at = ?, " +
                "closing_cash = ?, " +
                "expected_cash = ?, " +
                "cash_difference = ?, " +
                "total_sales = ?, " +
                "total_returns = ?, " +
                "status = ?, " +
                "approved_by = ?, " +
                "approved_at = ?, " +
                "notes = ? " +
                "WHERE shift_id = ?";

        jdbcTemplate.update(
                sql,
                shift.getClosedAt(),
                shift.getClosingCash(),
                shift.getExpectedCash(),
                shift.getCashDifference(),
                shift.getTotalSales(),
                shift.getTotalReturns(),
                shift.getStatus() != null ? shift.getStatus().name() : null,
                shift.getApprovedBy(),
                shift.getApprovedAt(),
                shift.getNotes(),
                shift.getShiftId()
        );
    }

    @Override
    public Optional<CashShift> findById(Long shiftId) {
        try {
            String sql = "SELECT * FROM cash_shifts WHERE shift_id = ?";
            CashShift shift = jdbcTemplate.queryForObject(sql, shiftRowMapper, shiftId);
            return Optional.ofNullable(shift);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CashShift> findOpenShiftByCashierId(Long cashierId) {
        try {
            String sql = "SELECT * FROM cash_shifts " +
                    "WHERE cashier_id = ? AND status IN ('OPEN', 'PENDING_APPROVAL') ORDER BY opened_at DESC LIMIT 1";
            CashShift shift = jdbcTemplate.queryForObject(sql, shiftRowMapper, cashierId);
            return Optional.ofNullable(shift);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasOpenShift(Long cashierId) {
        String sql = "SELECT COUNT(*) FROM cash_shifts " +
                "WHERE cashier_id = ? AND status IN ('OPEN', 'PENDING_APPROVAL')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cashierId);
        return count != null && count > 0;
    }

    @Override
    public Optional<CashShift> findByIdWithStats(Long shiftId) {
        try {
            String sql =
                    "SELECT cs.*, " +
                            "  COALESCE(COUNT(s.sale_id), 0) as transaction_count, " +
                            "  COALESCE(SUM(s.net_total), 0) as current_sales " +
                            "FROM cash_shifts cs " +
                            "LEFT JOIN sales s ON cs.shift_id = s.shift_id " +
                            "WHERE cs.shift_id = ? " +
                            "GROUP BY cs.shift_id";

            CashShift shift = jdbcTemplate.queryForObject(sql, shiftRowMapper, shiftId);
            return Optional.ofNullable(shift);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<CashShift> findByBranchId(Long branchId, int limit) {
        String sql = "SELECT * FROM cash_shifts " +
                "WHERE branch_id = ? " +
                "ORDER BY opened_at DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, shiftRowMapper, branchId, limit);
    }

    @Override
    public List<CashShift> findByCashierId(Long cashierId, int limit) {
        String sql = "SELECT * FROM cash_shifts " +
                "WHERE cashier_id = ? " +
                "ORDER BY opened_at DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, shiftRowMapper, cashierId, limit);
    }

    @Override
    public Optional<CashShift> findActiveShiftByCashier(Long cashierId) {
        try {
            String sql = "SELECT * FROM cash_shifts WHERE cashier_id = ? AND status = 'OPEN' ORDER BY opened_at DESC LIMIT 1";
            CashShift shift = jdbcTemplate.queryForObject(sql, shiftRowMapper, cashierId);
            return Optional.ofNullable(shift);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CashShift> findOpenShiftByBranchId(Long branchId) {
        try {
            String sql = "SELECT * FROM cash_shifts WHERE branch_id = ? AND status = 'OPEN' ORDER BY opened_at DESC LIMIT 1";
            CashShift shift = jdbcTemplate.queryForObject(sql, shiftRowMapper, branchId);
            return Optional.ofNullable(shift);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
