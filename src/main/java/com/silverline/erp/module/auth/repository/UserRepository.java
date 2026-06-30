package com.silverline.erp.module.auth.repository;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile; // Ensure User entity exists
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, Long> {
    // Corresponds to Step 2: SELECT * FROM user_profiles WHERE username = ...
    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findByRole(com.silverline.erp.domain.enums.Role role);

    List<UserProfile> findByUsernameContainingOrFullNameContaining(String username, String fullName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);


    Optional<UserProfile> findByEmail(String email);
}
