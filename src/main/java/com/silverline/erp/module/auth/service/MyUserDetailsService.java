package com.silverline.erp.module.auth.service;

import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import lombok.Data;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Data
@Component
@NullMarked
public class MyUserDetailsService implements UserDetailsService {

    private final UserProfileRepo userProfileRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserProfile> userProfile = userProfileRepo.findByUsername(username);
        UserProfile existUser = userProfile.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username: " + username));

        if (existUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Account is not active. Current status: " + existUser.getAccountStatus());
        }

        // NOTE: Branch active check REMOVED â€” users are NOT tied to branches
        return existUser;
    }
}

