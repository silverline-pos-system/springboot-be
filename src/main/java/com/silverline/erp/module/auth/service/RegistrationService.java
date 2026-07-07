package com.silverline.erp.module.auth.service;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;

import java.util.List;

public interface RegistrationService {
    RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO);
    List<Branch> getAllBranches();
}
