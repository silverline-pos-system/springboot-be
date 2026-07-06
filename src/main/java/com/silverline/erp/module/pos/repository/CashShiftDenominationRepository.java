package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.CashShiftDenomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashShiftDenominationRepository extends JpaRepository<CashShiftDenomination, Long> {
    List<CashShiftDenomination> findByShiftId(Long shiftId);

    List<CashShiftDenomination> findByShiftIdAndType(Long shiftId, CashShiftDenomination.DenominationType type);
}

