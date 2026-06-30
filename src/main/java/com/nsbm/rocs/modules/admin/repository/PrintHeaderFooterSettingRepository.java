package com.nsbm.rocs.modules.admin.repository;

import com.nsbm.rocs.entity.main.PrintHeaderFooterSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrintHeaderFooterSettingRepository extends JpaRepository<PrintHeaderFooterSetting, Long> {

    Optional<PrintHeaderFooterSetting> findByBranchId(Long branchId);

    void deleteByBranchId(Long branchId);
}


