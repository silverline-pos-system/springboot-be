package com.silverline.erp.module.manager.controller;

import com.silverline.erp.module.admin.service.SecondaryRoleService;
import com.silverline.erp.module.manager.dto.AssignSecondaryRoleRequest;
import com.silverline.erp.module.manager.dto.MySecondaryRoleResponse;
import com.silverline.erp.module.manager.dto.SecondaryRoleAssignmentDTO;
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
public class SecondaryRoleController {

    private final SecondaryRoleService secondaryRoleService;

    // GET /api/v1/manager/secondary-roles?branchId=1
    @GetMapping
    public ResponseEntity<List<SecondaryRoleAssignmentDTO>> getAssignments(
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching secondary role assignments for branchId: {}", branchId);
        List<SecondaryRoleAssignmentDTO> assignments = secondaryRoleService.getAssignments(branchId);
        return ResponseEntity.ok(assignments);
    }

    // POST /api/v1/manager/secondary-roles
    @PostMapping
    public ResponseEntity<SecondaryRoleAssignmentDTO> assignRole(
            @Valid @RequestBody AssignSecondaryRoleRequest request) {
        log.info("Assigning secondary role {} to userId: {}", request.getSecondaryRole(), request.getUserId());
        SecondaryRoleAssignmentDTO created = secondaryRoleService.assignRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // DELETE /api/v1/manager/secondary-roles/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeRole(@PathVariable Long id) {
        log.info("Revoking secondary role assignment id: {}", id);
        secondaryRoleService.revokeRole(id);
        return ResponseEntity.ok(Map.of("message", "Secondary role revoked successfully"));
    }

    // GET /api/v1/manager/secondary-roles/me
    // Override class-level auth — any logged-in user can call this
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MySecondaryRoleResponse> getMySecondaryRole(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching secondary role for user: {}", userDetails.getUsername());
        MySecondaryRoleResponse response = secondaryRoleService.getMySecondaryRole(userDetails);
        return ResponseEntity.ok(response);
    }
}
