package com.silverline.erp.module.auth.repo;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@NullMarked
public interface UserProfileRepo extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByEmail(String email);
    Optional<UserProfile> findByPhone(String phone);
    Optional<UserProfile> findByEmployeeId(String employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(CAST(SUBSTRING(u.employeeId, 4) as java.lang.Long)) FROM UserProfile u WHERE u.employeeId LIKE 'EMP%'")
    Long findMaxEmployeeIdSequence();

    // NOTE: findByBranch_BranchIdAndRole REMOVED â€” branch_id no longer on user_profiles
    List<UserProfile> findByRole(Role role);
}

