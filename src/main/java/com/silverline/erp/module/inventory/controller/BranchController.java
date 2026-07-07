package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/inventory/branches", "/api/inventory/branches"})
@RequiredArgsConstructor
@Tag(name = "Active Inventory Branches", description = "Public-facing inventory endpoints to fetch, create, and update branch locations")
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "Get all active branches", description = "Retrieves a list of all active store branch locations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branches list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDTO>>> getAllBranches() {
        log.info("Fetching all branches");
        List<BranchDTO> branches = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branches));
    }

    @Operation(summary = "Get branch by ID", description = "Looks up and returns branch profile details by branch database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branch details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDTO>> getBranchById(@PathVariable Long id) {
        log.info("Fetching branch ID: {}", id);
        BranchDTO branch = branchService.getBranchById(id);
        if (branch == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Branch not found with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branch));
    }

    @Operation(summary = "Create a new branch location", description = "Registers a new branch location in the inventory layout")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Branch profile created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation error")
    @PostMapping
    public ResponseEntity<ApiResponse<BranchDTO>> createBranch(@RequestBody BranchDTO branchDTO) {
        log.info("Creating branch: {}", branchDTO.getName());
        BranchDTO createdBranch = branchService.createBranch(branchDTO);
        return ResponseEntity.status(201).body(ApiResponse.success("Branch created successfully", createdBranch));
    }

    @Operation(summary = "Update branch profile", description = "Updates branch parameters (name, code, status, email) by branch ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branch profile updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDTO>> updateBranch(@PathVariable Long id, @RequestBody BranchDTO branchDTO) {
        log.info("Updating branch ID: {}", id);
        BranchDTO updatedBranch = branchService.updateBranch(id, branchDTO);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", updatedBranch));
    }

    @Operation(summary = "Delete branch configuration", description = "Deletes a branch configuration by database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branch configuration deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Deleting branch ID: {}", id);
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted successfully"));
    }
}

