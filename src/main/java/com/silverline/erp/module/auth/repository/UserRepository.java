package com.silverline.erp.module.auth.repository;

import com.silverline.erp.domain.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, Long> {
    // Corresponds to Step 2: SELECT * FROM user_profiles WHERE username = ...
    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findByRole(com.silverline.erp.domain.enums.Role role);

    List<UserProfile> findByUsernameContainingOrFullNameContaining(String username, String fullName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(CAST(SUBSTRING(REPLACE(u.employeeId, '-', ''), 4) as java.lang.Long)) FROM UserProfile u WHERE u.employeeId LIKE 'EMP%' AND u.employeeId NOT LIKE 'EMP_%'")
    Long findMaxEmployeeIdSequence();

    Optional<UserProfile> findByEmail(String email);
}
