package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/inventory/branches", "/api/inventory/branches"})
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDTO>>> getAllBranches() {
        log.info("Fetching all branches");
        List<BranchDTO> branches = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDTO>> getBranchById(@PathVariable Long id) {
        log.info("Fetching branch ID: {}", id);
        BranchDTO branch = branchService.getBranchById(id);
        if (branch == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Branch not found with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branch));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BranchDTO>> createBranch(@RequestBody BranchDTO branchDTO) {
        log.info("Creating branch: {}", branchDTO.getName());
        BranchDTO createdBranch = branchService.createBranch(branchDTO);
        return ResponseEntity.status(201).body(ApiResponse.success("Branch created successfully", createdBranch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDTO>> updateBranch(@PathVariable Long id, @RequestBody BranchDTO branchDTO) {
        log.info("Updating branch ID: {}", id);
        BranchDTO updatedBranch = branchService.updateBranch(id, branchDTO);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", updatedBranch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Deleting branch ID: {}", id);
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted successfully"));
    }
}
