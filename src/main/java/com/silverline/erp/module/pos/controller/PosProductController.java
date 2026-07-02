package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.service.BatchService;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import com.silverline.erp.module.inventory.service.ProductService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.PosProductDTO;
import com.silverline.erp.module.pos.service.QuickPickService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pos/products")
@CrossOrigin
@RequiredArgsConstructor
public class PosProductController {

    private final ProductService productService;
    private final StockService stockService;
    private final BatchService batchService;
    private final ProductSerialService productSerialService;
    private final QuickPickService quickPickService;

    // NOTE: branchId now comes from request parameters (user selects branch in POS)
    // Fallback to 1L for backward compatibility
    private Long getBranchIdFromParam(Long branchId) {
        return branchId != null ? branchId : 1L;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PosProductDTO>>> searchProducts(@RequestParam String q, @RequestParam(required = false) Long branchId) {
        if (q == null || q.trim().isEmpty()) {
            return new ResponseEntity<>(
                    ApiResponse.success("No query provided", List.of()),
                    HttpStatus.OK
            );
        }

        Long targetBranchId = getBranchIdFromParam(branchId);
        List<PosProductDTO> products = productService.searchProductEntities(q).stream()
                .limit(20)
                .map(p -> mapToDTO(p, targetBranchId))
                .collect(Collectors.toList());

        return new ResponseEntity<>(
                ApiResponse.success("Products found", products),
                HttpStatus.OK
        );
    }

    @GetMapping("/{query}")
    public ResponseEntity<ApiResponse<PosProductDTO>> getProduct(
            @PathVariable String query, 
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean skipStockCheck) {
        
        Long targetBranchId = getBranchIdFromParam(branchId);

        // 1. Try by ID (if numeric)
        if (query.matches("\\d+")) {
            try {
                Long id = Long.parseLong(query);
                var product = productService.findById(id);
                if (product != null) {
                    PosProductDTO dto = mapToDTO(product, targetBranchId);
                    // Check stock availability if not skipped
                    if (!skipStockCheck && dto.getAvailableStock() != null && dto.getAvailableStock().compareTo(BigDecimal.ZERO) <= 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Product '" + dto.getName() + "' is out of stock (Quantity: 0)"));
                    }
                    return ResponseEntity.ok(ApiResponse.success("Product found", dto));
                }
            } catch (NumberFormatException ignored) {}
        }

        // 2. Try by Barcode
        var productByBarcode = productService.findByBarcode(query);
        if (productByBarcode != null) {
            PosProductDTO dto = mapToDTO(productByBarcode, targetBranchId);
            if (!skipStockCheck && dto.getAvailableStock() != null && dto.getAvailableStock().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Product '" + dto.getName() + "' is out of stock (Quantity: 0)"));
            }
            return ResponseEntity.ok(ApiResponse.success("Product found", dto));
        }

        // 3. Try by SKU
        var productBySku = productService.findBySku(query);
        if (productBySku != null) {
            PosProductDTO dto = mapToDTO(productBySku, targetBranchId);
            if (!skipStockCheck && dto.getAvailableStock() != null && dto.getAvailableStock().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Product '" + dto.getName() + "' is out of stock (Quantity: 0)"));
            }
            return ResponseEntity.ok(ApiResponse.success("Product found", dto));
        }

        // 4. Try by Serial Number (IMEI)
        var serial = productSerialService.findSerialBySerialNo(query);
        if (serial != null) {
            var s = serial;
            if (!targetBranchId.equals(s.getBranchId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Serial '" + query + "' exists but belongs to another branch"));
            }
            if (!"IN_STOCK".equals(s.getStatus())) {
                // Return 400 unless skipping checks
                if (!skipStockCheck) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Serial '" + query + "' is already " + s.getStatus().toLowerCase()));
                }
            }
            
            var product = productService.findById(s.getProductId());
            if (product != null) {
                PosProductDTO dto = mapToDTO(product, targetBranchId);
                dto.setSelectedSerialId(s.getSerialId());
                dto.setSerialNo(s.getSerialNo());
                // For direct serial scans, quantity should be 1
                return ResponseEntity.ok(ApiResponse.success("Serial scanned: " + s.getSerialNo(), dto));
            }
        }

        return new ResponseEntity<>(
                ApiResponse.error("Product not found"),
                HttpStatus.NOT_FOUND);
    }

    @GetMapping("/quick")
    public ResponseEntity<ApiResponse<List<PosProductDTO>>> getQuickItems(@RequestParam(required = false) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        List<PosProductDTO> products = quickPickService.getQuickPickProducts(targetBranchId).stream()
                .map(p -> mapToDTO(p, targetBranchId))
                .collect(Collectors.toList());
        
        // Fallback to active items if none configured for branch
        if (products.isEmpty()) {
            products = productService.getActiveProductsLimit(10)
                    .stream()
                    .map(p -> mapToDTO(p, targetBranchId))
                    .collect(Collectors.toList());
        }

        return new ResponseEntity<>(
                ApiResponse.success("Quick items", products),
                HttpStatus.OK
        );
    }

    @PostMapping("/quick/{productId}")
    public ResponseEntity<ApiResponse<String>> addToQuickPick(@PathVariable Long productId, @RequestParam Long branchId) {
        quickPickService.addItem(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Added to quick pick", null));
    }

    @DeleteMapping("/quick/{productId}")
    public ResponseEntity<ApiResponse<String>> removeFromQuickPick(@PathVariable Long productId, @RequestParam Long branchId) {
        quickPickService.removeItem(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from quick pick", null));
    }

    /**
     * Get stock availability for a specific product
     */
    @GetMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<BigDecimal>> getProductStock(
            @PathVariable Long productId,
            @RequestParam(required = false) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        
        Integer stockVal = stockService.getCurrentStock(targetBranchId, productId);
        BigDecimal availableStock = stockVal != null ? BigDecimal.valueOf(stockVal) : BigDecimal.ZERO;
        
        return ResponseEntity.ok(ApiResponse.success("Stock fetched", availableStock));
    }

    private PosProductDTO mapToDTO(Product product, Long branchId) {
        PosProductDTO dto = new PosProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setSku(product.getSku());
        dto.setBarcode(product.getBarcode());
        dto.setIsSerialized(product.getIsSerialized());
        
        // Fetch available stock for the branch
        Integer stockVal = stockService.getCurrentStock(branchId, product.getProductId());
        BigDecimal availableStock = stockVal != null ? BigDecimal.valueOf(stockVal) : BigDecimal.ZERO;
        dto.setAvailableStock(availableStock);
        
        // Fetch available prices based on batches
        List<PosProductDTO.BatchPriceDTO> availablePrices = batchService.getFEFOBatches(product.getProductId(), branchId)
                .stream()
                .filter(b -> b.getSellingPrice() != null)
                .map(b -> new PosProductDTO.BatchPriceDTO(
                        b.getBatchId(),
                        b.getBatchCode(),
                        b.getSellingPrice(),
                        b.getMrp(),
                        b.getQty(),
                        b.getExpiryDate()
                ))
                .collect(Collectors.toList());
        dto.setAvailablePrices(availablePrices);
        
        return dto;
    }
}



