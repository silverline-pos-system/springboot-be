package com.silverline.erp.module.manager.controller;

import com.silverline.erp.module.admin.service.SecondaryRoleService;
import com.silverline.erp.module.manager.dto.AssignSecondaryRoleRequest;
import com.silverline.erp.module.manager.dto.MySecondaryRoleResponse;
import com.silverline.erp.module.manager.dto.SecondaryRoleAssignmentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager/secondary-roles")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Secondary Roles Overrides", description = "APIs for managers to temporarily assign or revoke secondary roles (e.g. Technician, Supervisor override permissions) to branch users")
public class SecondaryRoleController {

    private final SecondaryRoleService secondaryRoleService;

    @Operation(summary = "Get secondary role assignments", description = "Retrieves all active secondary role assignments in a branch (with optional branch filter)")
    @ApiResponse(responseCode = "200", description = "Secondary role assignments list fetched successfully")
    @GetMapping
    public ResponseEntity<List<SecondaryRoleAssignmentDTO>> getAssignments(
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching secondary role assignments for branchId: {}", branchId);
        List<SecondaryRoleAssignmentDTO> assignments = secondaryRoleService.getAssignments(branchId);
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Assign a secondary role", description = "Assigns a secondary role override (e.g. supervisor privileges) to a user for override validation tasks")
    @ApiResponse(responseCode = "201", description = "Secondary role assigned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid role string or validation error")
    @PostMapping
    public ResponseEntity<SecondaryRoleAssignmentDTO> assignRole(
            @Valid @RequestBody AssignSecondaryRoleRequest request) {
        log.info("Assigning secondary role {} to userId: {}", request.getSecondaryRole(), request.getUserId());
        SecondaryRoleAssignmentDTO created = secondaryRoleService.assignRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Revoke secondary role", description = "Revokes a secondary role assignment ID, resetting the user to default role permissions")
    @ApiResponse(responseCode = "200", description = "Secondary role assignment revoked successfully")
    @ApiResponse(responseCode = "404", description = "Assignment ID not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeRole(@PathVariable Long id) {
        log.info("Revoking secondary role assignment id: {}", id);
        secondaryRoleService.revokeRole(id);
        return ResponseEntity.ok(Map.of("message", "Secondary role revoked successfully"));
    }

    @Operation(summary = "Get my secondary role", description = "Retrieves active secondary roles configured for the current authenticated user context")
    @ApiResponse(responseCode = "200", description = "My secondary roles retrieved successfully")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MySecondaryRoleResponse> getMySecondaryRole(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching secondary role for user: {}", userDetails.getUsername());
        MySecondaryRoleResponse response = secondaryRoleService.getMySecondaryRole(userDetails);
        return ResponseEntity.ok(response);
    }
}
