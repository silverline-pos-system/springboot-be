package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.module.inventory.dto.ProductSerialDTO;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/v1/inventory/serials", "/api/inventory/serials"})
@RequiredArgsConstructor
@Tag(name = "Product Serial Numbers", description = "APIs for registering, editing, searching, and managing individual item serial numbers (IMEI, Device serials, etc.)")
public class ProductSerialController {

    private final ProductSerialService productSerialService;

    @Operation(summary = "Get all serial numbers", description = "Retrieves a paginated list of all product serial numbers in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serials list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<com.silverline.erp.common.dto.PagedResponse<ProductSerialDTO>>> getAllSerials(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<ProductSerialDTO> pageInfo = productSerialService.getAllSerials(pageable);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", com.silverline.erp.common.dto.PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get serials by product ID", description = "Lists all serial numbers registered for a specific product ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serials list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getSerialsByProduct(@PathVariable Long productId) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @Operation(summary = "Get serials by branch ID", description = "Lists all serial numbers currently stored at a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serials list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<?>> getSerialsByBranch(@PathVariable Long branchId) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @Operation(summary = "Get serials by status", description = "Lists all serial numbers matching a status code (e.g. AVAILABLE, SOLD, DAMAGED)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serials list retrieved successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<?>> getSerialsByStatus(@PathVariable String status) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @Operation(summary = "Get available serials", description = "Lists available serial numbers for a specific product and branch (used for POS selection)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available serials list retrieved successfully")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<?>> getAvailableSerials(
            @RequestParam Long branchId,
            @RequestParam Long productId) {
        List<ProductSerialDTO> serials = productSerialService.getAvailableSerials(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Available serials retrieved successfully", serials));
    }

    @Operation(summary = "Lookup serial numbers", description = "Performs dynamic searches and filters on serial numbers list with paging")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lookup search completed successfully")
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<?>> lookupSerials(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String branchIds,
            @RequestParam(required = false) String productIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Looking up serials with status: {}, search: {}", status, search);
        List<Long> parsedBranchIds = parseCsvIds(branchIds);
        List<Long> parsedProductIds = parseCsvIds(productIds);

        var result = productSerialService.lookupSerials(
                branchId,
                productId,
                parsedBranchIds,
                parsedProductIds,
                status,
                search,
                page,
                size
        );

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("items", result.getContent());
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", data));
    }

    @Operation(summary = "Get serial by database ID", description = "Retrieves serial details by serial database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial number not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getSerialById(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.getSerialById(id);
        return ResponseEntity.ok(ApiResponse.success("Serial retrieved successfully", serial));
    }

    @Operation(summary = "Get serial by barcode/serial number string", description = "Retrieves serial details by searching exact serial number string")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial number string not found")
    @GetMapping("/serial/{serialNo}")
    public ResponseEntity<ApiResponse<?>> getSerialBySerialNo(@PathVariable String serialNo) {
        ProductSerialDTO serial = productSerialService.getSerialBySerialNo(serialNo);
        return ResponseEntity.ok(ApiResponse.success("Serial retrieved successfully", serial));
    }

    @Operation(summary = "Register single serial number", description = "Registers a new product serial number in inventory")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Serial registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duplicate serial number or invalid payload parameters")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSerial(@Valid @RequestBody ProductSerialDTO serialDTO) {
        ProductSerialDTO createdSerial = productSerialService.createSerial(serialDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Serial created successfully", createdSerial));
    }

    @Operation(summary = "Register bulk serial numbers", description = "Creates multiple serial number records at once")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bulk serials registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Contains duplicate keys or invalid schema parameters")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<?>> createBulkSerials(
            @Valid @RequestBody List<ProductSerialDTO> serialDTOs) {
        List<ProductSerialDTO> createdSerials = productSerialService.createBulkSerials(serialDTOs);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Serials created successfully", createdSerials));
    }

    @Operation(summary = "Update serial details", description = "Modifies values (serial string, comments, status) of a serial record")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial record not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateSerial(
            @PathVariable Long id,
            @Valid @RequestBody ProductSerialDTO serialDTO) {
        ProductSerialDTO updatedSerial = productSerialService.updateSerial(id, serialDTO);
        return ResponseEntity.ok(ApiResponse.success("Serial updated successfully", updatedSerial));
    }

    @Operation(summary = "Mark serial as sold", description = "Changes status of a serial to SOLD and associates it with a sale transaction ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial status updated to SOLD successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial record not found")
    @PatchMapping("/{id}/sold")
    public ResponseEntity<ApiResponse<?>> markAsSold(
            @PathVariable Long id,
            @RequestParam Long saleId) {
        ProductSerialDTO serial = productSerialService.markAsSold(id, saleId);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as sold", serial));
    }

    @Operation(summary = "Mark serial as damaged", description = "Changes status of a serial number to DAMAGED")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial status updated to DAMAGED successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial record not found")
    @PatchMapping("/{id}/damaged")
    public ResponseEntity<ApiResponse<?>> markAsDamaged(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.markAsDamaged(id);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as damaged", serial));
    }

    @Operation(summary = "Mark serial as returned", description = "Changes status of a serial number back to AVAILABLE")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial status reset to AVAILABLE successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial record not found")
    @PatchMapping("/{id}/returned")
    public ResponseEntity<ApiResponse<?>> markAsReturned(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.markAsReturned(id);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as returned", serial));
    }

    @Operation(summary = "Delete serial record", description = "Deletes a serial number database entry by database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serial deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serial record not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSerial(@PathVariable Long id) {
        productSerialService.deleteSerial(id);
        return ResponseEntity.ok(ApiResponse.success("Serial deleted successfully"));
    }

    @Operation(summary = "Get damaged products summary list", description = "Retrieves counts and list of products logged as damaged (with optional branch filter)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Damaged products list retrieved successfully")
    @GetMapping("/damaged")
    public ResponseEntity<ApiResponse<?>> getDamagedProducts(
            @RequestParam(required = false) Long branchId) {
        List<DamagedProductDTO> damagedProducts = productSerialService.getDamagedProducts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Damaged products retrieved successfully", damagedProducts));
    }

    private List<Long> parseCsvIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}

