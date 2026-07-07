package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.PrintHeaderFooterDTO;
import com.silverline.erp.module.admin.service.PrintSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/print-settings")
@RequiredArgsConstructor
@Tag(name = "Print Header & Footer Overrides", description = "APIs to configure custom receipt header and footer settings per store branch")
public class PrintSettingsController {

    private final PrintSettingsService printSettingsService;

    @Operation(summary = "Get branch header/footer settings", description = "Retrieves print settings (business name, address, contact, policy notes) for a specific branch. Returns default empty fields if no override is configured.")
    @ApiResponse(responseCode = "200", description = "Settings retrieved successfully")
    @GetMapping("/header-footer")
    public ResponseEntity<PrintHeaderFooterDTO> getHeaderFooter(@RequestParam Long branchId) {
        log.info("Fetching print header/footer settings for branchId: {}", branchId);
        PrintHeaderFooterDTO dto = printSettingsService.getHeaderFooter(branchId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Save branch header/footer override", description = "Creates or updates receipt templates overrides (business name, address, footer policy message) for a specific store branch")
    @ApiResponse(responseCode = "200", description = "Print override settings saved successfully")
    @PutMapping("/header-footer")
    public ResponseEntity<PrintHeaderFooterDTO> saveHeaderFooter(
            @RequestParam Long branchId,
            @RequestBody PrintHeaderFooterDTO dto) {
        log.info("Saving print header/footer settings for branchId: {}", branchId);
        PrintHeaderFooterDTO saved = printSettingsService.saveHeaderFooter(branchId, dto);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Remove branch header/footer settings override", description = "Deletes the branch-specific override setting, forcing the branch to fallback to default system-wide layouts")
    @ApiResponse(responseCode = "200", description = "Settings override removed successfully")
    @ApiResponse(responseCode = "404", description = "Settings override not found for specified branch")
    @DeleteMapping("/header-footer")
    public ResponseEntity<?> deleteHeaderFooter(@RequestParam Long branchId) {
        log.info("Deleting print header/footer settings for branchId: {}", branchId);
        printSettingsService.deleteHeaderFooter(branchId);
        return ResponseEntity.ok(Map.of("message", "Print settings removed for branch " + branchId));
    }
}
