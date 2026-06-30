package com.silverline.erp.module.admin.repository;

import com.silverline.erp.domain.system.FeatureVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureVerificationCodeRepository extends JpaRepository<FeatureVerificationCode, Long> {

    Optional<FeatureVerificationCode> findTopByFeatureCodeAndActionAndIsUsedFalseOrderByCreatedAtDesc(
            String featureCode, String action);
}
