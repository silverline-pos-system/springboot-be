package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.DispatchDTO;
import com.silverline.erp.module.admin.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("adminDispatchController")
@RequestMapping("/api/v1/admin/dispatches")
public class DispatchController {

    private final DispatchService dispatchService;

    @Autowired
    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /**
     * GET /api/v1/admin/dispatches/pending
     * Returns all pending Dispatches across all branches.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<DispatchDTO>> getAllPendingDispatches() {
        List<DispatchDTO> list = dispatchService.getAllPendingDispatches();
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/v1/admin/dispatches/count/pending?branchId={branchId}
     * If branchId is provided returns count for that branch, otherwise returns total pending count.
     */
    @GetMapping("/count/pending")
    public ResponseEntity<Long> getPendingDispatchCount(@RequestParam(value = "branchId", required = false) Long branchId) {
        Long count = (branchId == null) ? dispatchService.getPendingDispatchCountAll() : dispatchService.getPendingDispatchCount(branchId);
        return ResponseEntity.ok(count);
    }
}
