package com.nsbm.rocs.modules.auth.repo;

import com.nsbm.rocs.entity.main.Branch;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepo extends JpaRepository<@NonNull Branch, @NonNull Long> {
}

