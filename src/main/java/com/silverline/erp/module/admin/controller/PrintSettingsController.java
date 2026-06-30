package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.PrintHeaderFooterDTO;
import com.silverline.erp.module.admin.service.PrintSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/print-settings")
@RequiredArgsConstructor
public class PrintSettingsController {

    private final PrintSettingsService printSettingsService;

    /**
     * GET /api/v1/admin/print-settings/header-footer?branchId={id}
     * Returns header/footer settings for the given branch.
     */
    @GetMapping("/header-footer")
    public ResponseEntity<PrintHeaderFooterDTO> getHeaderFooter(@RequestParam Long branchId) {
        log.info("Fetching print header/footer settings for branchId: {}", branchId);
        PrintHeaderFooterDTO dto = printSettingsService.getHeaderFooter(branchId);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /api/v1/admin/print-settings/header-footer?branchId={id}
     * Create or update header/footer settings for the given branch.
     */
    @PutMapping("/header-footer")
    public ResponseEntity<PrintHeaderFooterDTO> saveHeaderFooter(
            @RequestParam Long branchId,
            @RequestBody PrintHeaderFooterDTO dto) {
        log.info("Saving print header/footer settings for branchId: {}", branchId);
        PrintHeaderFooterDTO saved = printSettingsService.saveHeaderFooter(branchId, dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/v1/admin/print-settings/header-footer?branchId={id}
     * Remove the branch-specific override so it falls back to defaults.
     */
    @DeleteMapping("/header-footer")
    public ResponseEntity<?> deleteHeaderFooter(@RequestParam Long branchId) {
        log.info("Deleting print header/footer settings for branchId: {}", branchId);
        printSettingsService.deleteHeaderFooter(branchId);
        return ResponseEntity.ok(Map.of("message", "Print settings removed for branch " + branchId));
    }
}


