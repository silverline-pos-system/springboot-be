package com.silverline.erp.module.admin.repository;

import com.silverline.erp.domain.system.PrintHeaderFooterSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrintHeaderFooterSettingRepository extends JpaRepository<PrintHeaderFooterSetting, Long> {

    Optional<PrintHeaderFooterSetting> findByBranchId(Long branchId);

    void deleteByBranchId(Long branchId);
}


