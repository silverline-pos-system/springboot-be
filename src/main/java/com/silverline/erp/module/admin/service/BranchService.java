package com.silverline.erp.module.admin.service;

import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.dto.UserDTO;

import java.util.List;

public interface BranchService {

    BranchDTO createBranch(BranchDTO dto);

    List<BranchDTO> getAllBranches();

    BranchDTO getBranchById(Long id);

    BranchDTO updateBranch(Long id, BranchDTO dto);

    void deleteBranch(Long id);

    void toggleBranchStatus(Long id);

    java.util.Map<String, Object> getBranchSummary(Long id);

    java.util.Map<String, Object> getBranchRealTimeSales(Long id);

    List<UserDTO> getUsersByBranchId(Long id);
}

