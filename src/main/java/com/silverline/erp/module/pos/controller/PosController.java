package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.dto.sale.SaleSummaryDTO;
import com.silverline.erp.module.pos.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.silverline.erp.module.admin.service.PrintSettingsService;
import com.silverline.erp.module.admin.dto.PrintHeaderFooterDTO;

@Slf4j
@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@Tag(name = "POS Checkout", description = "Point of Sale transaction, billing, returns, and loyalty discount processing APIs")
public class PosController {

    private final PosSaleService saleService;
    private final ReturnService returnService;
    private final LoyaltyService loyaltyService;
    private final SaleQueryService saleQueryService;
    private final ShiftService shiftService;
    private final PrintSettingsService printSettingsService;

    @Operation(summary = "Get last invoice info", description = "Retrieves metadata about the last recorded sale/invoice in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice info fetched successfully")
    @GetMapping({"/sales/last-invoice", "/orders/last-invoice"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLastInvoice() {
        log.info("Fetching last invoice info");
        Map<String, Object> invoiceInfo = saleQueryService.getLastInvoiceInfo();
        return ResponseEntity.ok(ApiResponse.success("Invoice info fetched", invoiceInfo));
    }

    @Operation(summary = "Submit checkout order / sale", description = "Deducts stock, logs payments, and records a new completed sale in the current cashier shift")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No active shift, invalid request format, or business validation failure")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Cashier not authenticated")
    @PostMapping({"/orders", "/sales"})
    public ResponseEntity<ApiResponse<SaleResponse>> submitOrder(@Valid @RequestBody com.silverline.erp.module.pos.dto.sale.CreateSaleRequest request) {
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

        SaleResponse response = saleService.createSale(request, branchId, cashierId, shiftId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order created successfully", response));
    }

    @Operation(summary = "Price a cart", description = "Returns the live priced cart with batch prices and promotions applied, without persisting anything")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart priced successfully")
    @PostMapping("/cart/price")
    public ResponseEntity<ApiResponse<com.silverline.erp.module.pos.dto.CartPricing.Response>> priceCart(
            @RequestBody com.silverline.erp.module.pos.dto.CartPricing.Request request) {
        if (request.getBranchId() == null) {
            request.setBranchId(1L);
        }
        return ResponseEntity.ok(ApiResponse.success("Cart priced", saleService.priceCart(request)));
    }

    @Operation(summary = "Get sales list / bills", description = "Returns a paginated list of sales records for the branch, with optional filters")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders fetched successfully")
    @GetMapping({"/orders", "/sales"})
    public ResponseEntity<ApiResponse<PagedResponse<SaleSummaryDTO>>> getBills(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Fetching bills with status: {}, pageable: {}", status, pageable);
        Long branchId = 1L;
        Page<SaleSummaryDTO> pageInfo = saleQueryService.getSaleSummaries(branchId, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get sale bill by ID", description = "Retrieves full sale, items, and payments details by sale ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order fetched successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bill not found")
    @GetMapping({"/orders/{id}", "/sales/{id}"})
    public ResponseEntity<ApiResponse<SaleResponse>> getBillById(@PathVariable Long id) {
        log.info("Fetching bill ID: {}", id);
        SaleResponse response = saleQueryService.getSaleById(id);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Bill not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Order fetched", response));
    }

    @Operation(summary = "Put bill on hold", description = "Saves a draft checkout request as a HELD bill for later retrieval")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bill held successfully")
    @PostMapping("/sales/hold")
    public ResponseEntity<ApiResponse<SaleResponse>> holdBill(@Valid @RequestBody com.silverline.erp.module.pos.dto.sale.CreateSaleRequest request) {
        log.info("Holding bill for branch: {}", request.getBranchId());
        request.setStatus("HELD");
        return submitOrder(request);
    }

    @Operation(summary = "Get held bills list", description = "Fetches a list of all currently held (draft) bills for the branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fetched held bills successfully")
    @GetMapping("/sales/held")
    public ResponseEntity<ApiResponse<List<SaleSummaryDTO>>> getHeldBills(@RequestParam(required = false) Long branchId) {
        log.info("Fetching held bills for branch: {}", branchId);
        return ResponseEntity.ok(ApiResponse.success("Fetched held bills", saleQueryService.getHeldBills(branchId)));
    }

    @Operation(summary = "Recall held bill", description = "Sets status of a held bill to PENDING so it can be resumed at the POS terminal")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Held bill recalled successfully")
    @PostMapping("/sales/{id}/recall")
    public ResponseEntity<ApiResponse<SaleResponse>> recallBill(@PathVariable Long id) {
        log.info("Recalling bill ID: {}", id);
        saleService.updateSaleStatus(id, "PENDING");
        return getBillById(id);
    }

    @Operation(summary = "Get returnable sales list", description = "Retrieves sales that are eligible for processing returns based on threshold policy")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returnable sales fetched successfully")
    @GetMapping("/sales/returnable")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getReturnableSales(
            @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        log.info("Fetching returnable sales for last {} days", days);
        Long branchId = 1L;
        List<SaleResponse> sales = saleQueryService.getReturnableSales(branchId, days);
        return ResponseEntity.ok(ApiResponse.success("Returnable sales fetched", sales));
    }

    @Operation(summary = "Get sale details by Invoice Number", description = "Looks up a specific sale by its unique invoice number identifier")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sale fetched successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    @GetMapping("/sales/invoice/{invoiceNo}")
    public ResponseEntity<ApiResponse<SaleResponse>> getSaleByInvoice(@PathVariable String invoiceNo) {
        log.info("Searching sale by invoice: {}", invoiceNo);
        SaleResponse response = saleQueryService.getSaleByInvoiceNo(invoiceNo);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Invoice not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Sale fetched", response));
    }

    @Operation(summary = "Process sales return", description = "Validates items and refund policy, adds items back to stock, and logs supervisor verification")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Return processed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Supervisor validation failure or restocking error")
    @PostMapping("/returns")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processReturn(@Valid @RequestBody com.silverline.erp.module.pos.dto.returns.ReturnRequest request) {
        log.info("Processing return for sale ID: {}", request.getSaleId());
        Long returnId = returnService.processReturn(request);
        return ResponseEntity.ok(ApiResponse.success("Return processed successfully", Map.of("returnId", returnId)));
    }

    @Operation(summary = "Request loyalty redemption code", description = "Triggers sending a one-time OTP verification code to the customer for point redemption")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification code sent successfully")
    @PostMapping("/customers/loyalty/request-redeem")
    public ResponseEntity<ApiResponse<String>> requestLoyaltyRedemption(@Valid @RequestBody com.silverline.erp.module.pos.dto.customer.LoyaltyRedeemRequest request) {
        log.info("Requesting loyalty redemption for customer: {}", request.getCustomerId());
        loyaltyService.requestLoyaltyRedemption(request.getCustomerId(), request.getPointsToRedeem());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
    }

    @Operation(summary = "Verify loyalty redemption OTP", description = "Validates the customer's verification code and returns the applied discount value")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Redemption verified successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code or insufficient points")
    @PostMapping("/customers/loyalty/verify-redeem")
    public ResponseEntity<ApiResponse<Map<String, java.math.BigDecimal>>> verifyLoyaltyRedemption(@Valid @RequestBody com.silverline.erp.module.pos.dto.customer.LoyaltyRedeemVerifyRequest request) {
        log.info("Verifying loyalty redemption for customer: {}", request.getCustomerId());
        java.math.BigDecimal discountValue = loyaltyService.verifyLoyaltyRedemption(request.getCustomerId(), request.getPointsToRedeem(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success("Redemption verified", Map.of("discountValue", discountValue)));
    }

    @Operation(summary = "Get print header/footer settings for POS", description = "Retrieves print settings (business name, address, contact, policy notes) for a specific branch. Allowed for cashier role.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settings retrieved successfully")
    @GetMapping("/print-settings/header-footer")
    public ResponseEntity<ApiResponse<PrintHeaderFooterDTO>> getPrintSettings(@RequestParam Long branchId) {
        log.info("POS fetching print header/footer settings for branchId: {}", branchId);
        PrintHeaderFooterDTO dto = printSettingsService.getHeaderFooter(branchId);
        return ResponseEntity.ok(ApiResponse.success("Print settings retrieved", dto));
    }
}
