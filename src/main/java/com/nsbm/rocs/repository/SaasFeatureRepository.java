package com.nsbm.rocs.repository;

import com.nsbm.rocs.entity.main.SaasFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaasFeatureRepository extends JpaRepository<SaasFeature, Long> {

    Optional<SaasFeature> findByFeatureCode(String featureCode);

    List<SaasFeature> findByIsActiveTrue();

    List<SaasFeature> findByFeatureCategory(String category);

    List<SaasFeature> findAllByOrderByFeatureCategoryAscFeatureNameAsc();

    boolean existsByFeatureCode(String featureCode);
}
