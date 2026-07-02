package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.service.impl.BranchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/branches")
@RequiredArgsConstructor
public class AdminBranchController {

    private final BranchServiceImpl branchService;

    @PostMapping("")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchDTO dto) {
        BranchDTO created = branchService.createBranch(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping({"", "/getAll"})
    public ResponseEntity<List<BranchDTO>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @RequestBody BranchDTO dto) {
        return ResponseEntity.ok(branchService.updateBranch(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleBranchStatus(@PathVariable Long id) {
        branchService.toggleBranchStatus(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<java.util.Map<String, Object>> getBranchSummary(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchSummary(id));
    }

    @GetMapping("/{id}/realtime-sales")
    public ResponseEntity<java.util.Map<String, Object>> getBranchRealTimeSales(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchRealTimeSales(id));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserDTO>> getUsersByBranch(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getUsersByBranchId(id));
    }
}

