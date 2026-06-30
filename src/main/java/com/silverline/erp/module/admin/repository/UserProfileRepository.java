package com.silverline.erp.module.admin.repository;

import com.silverline.erp.domain.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserProfile entity operations
 * Provides methods for querying staff data
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {


    /**
     * Find by username
     */
    Optional<UserProfile> findByUsername(String username);

    /**
     * Find active users (global)
     */
    @Query("SELECT u FROM UserProfile u WHERE u.accountStatus = 'ACTIVE' ORDER BY u.lastLogin DESC")
    List<UserProfile> findActiveUsersByBranchId(@Param("branchId") Long branchId);

    /**
     * Count active users (global)
     */
    @Query("SELECT COUNT(u) FROM UserProfile u WHERE u.accountStatus = 'ACTIVE'")
    Long countActiveUsersByBranchId(@Param("branchId") Long branchId);
}


