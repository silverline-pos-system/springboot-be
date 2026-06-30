package com.nsbm.rocs.modules.pos.controller;

import com.nsbm.rocs.modules.pos.dto.sale.SaleResponse;
import com.nsbm.rocs.modules.pos.dto.sale.SaleSummaryDTO;
import com.nsbm.rocs.modules.pos.service.PosService;
import com.nsbm.rocs.modules.pos.service.ShiftService;
import com.nsbm.rocs.shared.response.ApiResponse;
import com.nsbm.rocs.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@CrossOrigin
public class PosController {

    private final PosService posService;
    private final ShiftService shiftService;

    @GetMapping({"/sales/last-invoice", "/orders/last-invoice"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLastInvoice() {
        log.info("Fetching last invoice info");
        Map<String, Object> invoiceInfo = posService.getLastInvoiceInfo();
        return ResponseEntity.ok(ApiResponse.success("Invoice info fetched", invoiceInfo));
    }

    @PostMapping({"/orders", "/sales"})
    public ResponseEntity<ApiResponse<SaleResponse>> submitOrder(@RequestBody com.nsbm.rocs.modules.pos.dto.sale.CreateSaleRequest request) {
        Long cashierId = SecurityUtils.getCurrentUserId();
        if (cashierId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        log.info("Processing sale for branch: {}, cashier: {}", request.getBranchId(), cashierId);
        Long branchId = request.getBranchId() != null ? request.getBranchId() : 1L;

        Long shiftId;
        try {
            shiftId = shiftService.getActiveShiftId(cashierId);
        } catch (IllegalStateException e) {
            log.warn("No active shift for cashier: {}", cashierId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }

        SaleResponse response = posService.createSale(request, branchId, cashierId, shiftId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping({"/orders", "/sales"})
    public ResponseEntity<ApiResponse<List<SaleSummaryDTO>>> getBills(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        log.info("Fetching bills with status: {}", status);
        Long branchId = 1L; // Should ideally come from context or header
        List<SaleSummaryDTO> response = posService.getSaleSummaries(branchId, status, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", response));
    }

    @GetMapping({"/orders/{id}", "/sales/{id}"})
    public ResponseEntity<ApiResponse<SaleResponse>> getBillById(@PathVariable Long id) {
        log.info("Fetching bill ID: {}", id);
        SaleResponse response = posService.getSaleById(id);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Bill not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Order fetched", response));
    }

    @PostMapping("/sales/hold")
    public ResponseEntity<ApiResponse<SaleResponse>> holdBill(@RequestBody com.nsbm.rocs.modules.pos.dto.sale.CreateSaleRequest request) {
        log.info("Holding bill for branch: {}", request.getBranchId());
        request.setStatus("HELD");
        return submitOrder(request);
    }

    @GetMapping("/sales/held")
    public ResponseEntity<ApiResponse<List<SaleSummaryDTO>>> getHeldBills(@RequestParam(required = false) Long branchId) {
        log.info("Fetching held bills for branch: {}", branchId);
        return ResponseEntity.ok(ApiResponse.success("Fetched held bills", posService.getHeldBills(branchId)));
    }

    @PostMapping("/sales/{id}/recall")
    public ResponseEntity<ApiResponse<SaleResponse>> recallBill(@PathVariable Long id) {
        log.info("Recalling bill ID: {}", id);
        posService.updateSaleStatus(id, "PENDING");
        return getBillById(id);
    }

    @GetMapping("/sales/returnable")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getReturnableSales(
            @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        log.info("Fetching returnable sales for last {} days", days);
        Long branchId = 1L; 
        List<SaleResponse> sales = posService.getReturnableSales(branchId, days);
        return ResponseEntity.ok(ApiResponse.success("Returnable sales fetched", sales));
    }

    @GetMapping("/sales/invoice/{invoiceNo}")
    public ResponseEntity<ApiResponse<SaleResponse>> getSaleByInvoice(@PathVariable String invoiceNo) {
        log.info("Searching sale by invoice: {}", invoiceNo);
        SaleResponse response = posService.getSaleByInvoiceNo(invoiceNo);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Invoice not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Sale fetched", response));
    }

    @PostMapping("/returns")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processReturn(@RequestBody com.nsbm.rocs.modules.pos.dto.returns.ReturnRequest request) {
        log.info("Processing return for sale ID: {}", request.getSaleId());
        Long returnId = posService.processReturn(request);
        return ResponseEntity.ok(ApiResponse.success("Return processed successfully", Map.of("returnId", returnId)));
    }

    @PostMapping("/customers/loyalty/request-redeem")
    public ResponseEntity<ApiResponse<String>> requestLoyaltyRedemption(@RequestBody com.nsbm.rocs.modules.pos.dto.customer.LoyaltyRedeemRequest request) {
        log.info("Requesting loyalty redemption for customer: {}", request.getCustomerId());
        posService.requestLoyaltyRedemption(request.getCustomerId(), request.getPointsToRedeem());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
    }

    @PostMapping("/customers/loyalty/verify-redeem")
    public ResponseEntity<ApiResponse<Map<String, java.math.BigDecimal>>> verifyLoyaltyRedemption(@RequestBody com.nsbm.rocs.modules.pos.dto.customer.LoyaltyRedeemVerifyRequest request) {
        log.info("Verifying loyalty redemption for customer: {}", request.getCustomerId());
        java.math.BigDecimal discountValue = posService.verifyLoyaltyRedemption(request.getCustomerId(), request.getPointsToRedeem(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success("Redemption verified", Map.of("discountValue", discountValue)));
    }
}
