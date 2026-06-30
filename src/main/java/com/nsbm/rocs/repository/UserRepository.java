package com.nsbm.rocs.repository;

import com.nsbm.rocs.entity.main.UserProfile; // Ensure User entity exists
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, Long> {
    // Corresponds to Step 2: SELECT * FROM user_profiles WHERE username = ...
    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findByRole(com.nsbm.rocs.entity.enums.Role role);

    List<UserProfile> findByUsernameContainingOrFullNameContaining(String username, String fullName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);


    Optional<UserProfile> findByEmail(String email);
}
