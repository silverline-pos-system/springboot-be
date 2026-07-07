package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerUserRepository extends JpaRepository<UserProfile, Long> {

    // NOTE: Users are not tied to branches. Returning all active/rejected users if queried by branch.
    @Query("SELECT u FROM UserProfile u WHERE u.accountStatus = 'ACTIVE' OR u.accountStatus = 'REJECTED'")
    List<UserProfile> findActiveByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT u FROM UserProfile u WHERE u.accountStatus = 'ACTIVE' OR u.accountStatus = 'REJECTED'")
    List<UserProfile> findAllActive();
}


