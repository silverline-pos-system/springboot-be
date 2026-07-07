package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.service.impl.BranchServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/branches")
@RequiredArgsConstructor
@Tag(name = "Branch Administration", description = "APIs for administrators to create, update, disable, and monitor branch statistics and employees")
public class AdminBranchController {

    private final BranchServiceImpl branchService;

    @Operation(summary = "Create a new branch", description = "Registers a new branch location in the system database")
    @ApiResponse(responseCode = "201", description = "Branch created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or validation error")
    @PostMapping("")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchDTO dto) {
        BranchDTO created = branchService.createBranch(dto);
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "Get all branches list", description = "Retrieves all branches configured in the system (includes active and inactive branches)")
    @ApiResponse(responseCode = "200", description = "Branches list retrieved successfully")
    @GetMapping({"", "/getAll"})
    public ResponseEntity<List<BranchDTO>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @Operation(summary = "Get branch by ID", description = "Retrieves profile and layout configurations for a specific branch database ID")
    @ApiResponse(responseCode = "200", description = "Branch profile fetched successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @Operation(summary = "Update branch profile", description = "Updates details (name, code, address, manager) for an existing branch ID")
    @ApiResponse(responseCode = "200", description = "Branch profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @RequestBody BranchDTO dto) {
        return ResponseEntity.ok(branchService.updateBranch(id, dto));
    }

    @Operation(summary = "Delete branch location", description = "Removes a branch location record from the system registry")
    @ApiResponse(responseCode = "204", description = "Branch deleted successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle branch active status", description = "Toggles branch between active and inactive states. Inactivating a branch blocks cashiers from opening shifts there.")
    @ApiResponse(responseCode = "200", description = "Branch active status toggled successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleBranchStatus(@PathVariable Long id) {
        branchService.toggleBranchStatus(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get branch performance summary", description = "Retrieves sales statistics, inventory levels, and shift performance summaries for a specific branch")
    @ApiResponse(responseCode = "200", description = "Branch performance summary compiled successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getBranchSummary(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchSummary(id));
    }

    @Operation(summary = "Get real-time sales overview", description = "Retrieves live sales figures and checkout counts for a branch during the current operating day")
    @ApiResponse(responseCode = "200", description = "Real-time sales overview compiled successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @GetMapping("/{id}/realtime-sales")
    public ResponseEntity<Map<String, Object>> getBranchRealTimeSales(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchRealTimeSales(id));
    }

    @Operation(summary = "Get users registered in branch", description = "Lists profiles for all employee accounts assigned to the specified branch location")
    @ApiResponse(responseCode = "200", description = "Branch employee list retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Branch location not found")
    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserDTO>> getUsersByBranch(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getUsersByBranchId(id));
    }
}
