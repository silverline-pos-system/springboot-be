package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.module.inventory.dto.ProductSerialDTO;
import com.silverline.erp.module.inventory.service.ProductSerialService;
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
public class ProductSerialController {

    private final ProductSerialService productSerialService;

    @GetMapping
    public ResponseEntity<ApiResponse<com.silverline.erp.common.dto.PagedResponse<ProductSerialDTO>>> getAllSerials(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<ProductSerialDTO> pageInfo = productSerialService.getAllSerials(pageable);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", com.silverline.erp.common.dto.PagedResponse.from(pageInfo)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getSerialsByProduct(@PathVariable Long productId) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<?>> getSerialsByBranch(@PathVariable Long branchId) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<?>> getSerialsByStatus(@PathVariable String status) {
        List<ProductSerialDTO> serials = productSerialService.getSerialsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Serials retrieved successfully", serials));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<?>> getAvailableSerials(
            @RequestParam Long branchId,
            @RequestParam Long productId) {
        List<ProductSerialDTO> serials = productSerialService.getAvailableSerials(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Available serials retrieved successfully", serials));
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getSerialById(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.getSerialById(id);
        return ResponseEntity.ok(ApiResponse.success("Serial retrieved successfully", serial));
    }

    @GetMapping("/serial/{serialNo}")
    public ResponseEntity<ApiResponse<?>> getSerialBySerialNo(@PathVariable String serialNo) {
        ProductSerialDTO serial = productSerialService.getSerialBySerialNo(serialNo);
        return ResponseEntity.ok(ApiResponse.success("Serial retrieved successfully", serial));
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

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSerial(@Valid @RequestBody ProductSerialDTO serialDTO) {
        ProductSerialDTO createdSerial = productSerialService.createSerial(serialDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Serial created successfully", createdSerial));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<?>> createBulkSerials(
            @Valid @RequestBody List<ProductSerialDTO> serialDTOs) {
        List<ProductSerialDTO> createdSerials = productSerialService.createBulkSerials(serialDTOs);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Serials created successfully", createdSerials));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateSerial(
            @PathVariable Long id,
            @Valid @RequestBody ProductSerialDTO serialDTO) {
        ProductSerialDTO updatedSerial = productSerialService.updateSerial(id, serialDTO);
        return ResponseEntity.ok(ApiResponse.success("Serial updated successfully", updatedSerial));
    }

    @PatchMapping("/{id}/sold")
    public ResponseEntity<ApiResponse<?>> markAsSold(
            @PathVariable Long id,
            @RequestParam Long saleId) {
        ProductSerialDTO serial = productSerialService.markAsSold(id, saleId);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as sold", serial));
    }

    @PatchMapping("/{id}/damaged")
    public ResponseEntity<ApiResponse<?>> markAsDamaged(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.markAsDamaged(id);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as damaged", serial));
    }

    @PatchMapping("/{id}/returned")
    public ResponseEntity<ApiResponse<?>> markAsReturned(@PathVariable Long id) {
        ProductSerialDTO serial = productSerialService.markAsReturned(id);
        return ResponseEntity.ok(ApiResponse.success("Serial marked as returned", serial));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSerial(@PathVariable Long id) {
        productSerialService.deleteSerial(id);
        return ResponseEntity.ok(ApiResponse.success("Serial deleted successfully"));
    }

    @GetMapping("/damaged")
    public ResponseEntity<ApiResponse<?>> getDamagedProducts(
            @RequestParam(required = false) Long branchId) {
        List<DamagedProductDTO> damagedProducts = productSerialService.getDamagedProducts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Damaged products retrieved successfully", damagedProducts));
    }
}
