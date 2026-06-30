package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategoryId(Long categoryId);

    List<SubCategory> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<SubCategory> findByIsActiveTrue();

    boolean existsByNameAndCategoryId(String name, Long categoryId);
}


