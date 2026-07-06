package com.silverline.erp.module.auth.service;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;

import java.util.List;

public interface AuthService {
    UserProfile findByEmail(String email);

    UserProfile findByUsername(String username);

    List<Branch> getAllBranches();

    RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO);

    LogInResponseDTO logInUser(String username, String password);

    boolean verifySupervisor(String username, String password);

    void forgotPassword(String username, String newPassword, String reason);

    void verifyForgotPasswordToken(String username, String token);
}
