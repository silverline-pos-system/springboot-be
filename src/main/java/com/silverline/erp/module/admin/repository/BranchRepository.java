package com.silverline.erp.module.admin.repository;

import com.silverline.erp.domain.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    List<Branch> findByIsActiveTrue();
    boolean existsByCode(String code);
    boolean existsByCodeAndBranchIdNot(String code, Long branchId);
    boolean existsByName(String name);
    boolean existsByNameAndBranchIdNot(String name, Long branchId);
}

