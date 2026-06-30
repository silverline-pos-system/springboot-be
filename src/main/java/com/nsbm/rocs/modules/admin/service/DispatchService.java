package com.nsbm.rocs.modules.admin.service;

import com.nsbm.rocs.modules.admin.dto.DispatchDTO;

import java.util.List;

public interface DispatchService {
    /**
     * Return the count of pending Dispatches for the given branch.
     */
    Long getPendingDispatchCount(Long branchId);

    /**
     * Return the total count of pending Dispatches across all branches.
     */
    Long getPendingDispatchCountAll();

    /**
     * Return all pending Dispatches across all branches mapped to DispatchDTO.
     */
    List<DispatchDTO> getAllPendingDispatches();
}

