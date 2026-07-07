package com.silverline.erp.module.auth.service;

import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;

public interface AuthService {
    UserProfile findByEmail(String email);

    UserProfile findByUsername(String username);

    LogInResponseDTO logInUser(String username, String password);

    boolean verifySupervisor(String username, String password);
}
