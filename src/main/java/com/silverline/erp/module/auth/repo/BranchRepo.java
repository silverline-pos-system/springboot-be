package com.silverline.erp.module.auth.repo;

import com.silverline.erp.domain.branch.Branch;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepo extends JpaRepository<@NonNull Branch, @NonNull Long> {
}

