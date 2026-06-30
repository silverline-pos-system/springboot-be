package com.nsbm.rocs.repository;

import com.nsbm.rocs.entity.main.FeatureVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureVerificationCodeRepository extends JpaRepository<FeatureVerificationCode, Long> {

    Optional<FeatureVerificationCode> findTopByFeatureCodeAndActionAndIsUsedFalseOrderByCreatedAtDesc(
            String featureCode, String action);
}
