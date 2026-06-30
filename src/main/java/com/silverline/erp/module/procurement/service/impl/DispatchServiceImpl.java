package com.silverline.erp.module.procurement.service.impl;

import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Unit;
import com.silverline.erp.module.procurement.dto.DispatchCreateRequestDTO;
import com.silverline.erp.module.procurement.dto.DispatchFilterDTO;
import com.silverline.erp.module.procurement.dto.DispatchItemDTO;
import com.silverline.erp.module.procurement.dto.DispatchResponseDTO;
import com.silverline.erp.module.procurement.dto.DispatchStatsDTO;
import com.silverline.erp.module.procurement.dto.DispatchUpdateRequestDTO;
import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.domain.inventory.DispatchItem;
import com.silverline.erp.domain.inventory.ProductSerial;
import com.silverline.erp.domain.inventory.PurchaseOrder;
import com.silverline.erp.domain.inventory.PurchaseOrderItem;
import com.silverline.erp.domain.inventory.Product;
import com.silverline.erp.domain.inventory.Supplier;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.module.inventory.dto.*;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.common.exception.DispatchException;
import com.silverline.erp.module.procurement.repository.DispatchRepository;
import com.silverline.erp.module.procurement.repository.DispatchItemRepository;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.inventory.repository.InventoryStockRepository;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.ProductSerialRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderItemRepository;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.procurement.service.DispatchService;
import com.silverline.erp.module.procurement.service.DispatchPaymentRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("inventoryDispatchService")
@Transactional
@Slf4j
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private DispatchItemRepository dispatchItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryStockRepository stockRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private ProductSerialRepository productSerialRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository poItemRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private DispatchPaymentRequestService paymentRequestService;

    // ========================================================================
    //  DISPATCH CREATION â€” with full PO + IMEI + FEFO validation gates
    // ========================================================================

    @Override
    public DispatchResponseDTO createDispatch(DispatchCreateRequestDTO request, Long currentUserId) {
        log.info("Creating dispatch for branch: {}, supplier: {}, poId: {}",
                request.getBranchId(), request.getSupplierId(), request.getPoId());

        // â”€â”€â”€â”€ GATE 0: Basic item-level validation (null, qty, price) â”€â”€â”€â”€
        validateDispatchItems(request.getItems());

        // â”€â”€â”€â”€ Validate supplier exists and is active â”€â”€â”€â”€
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new DispatchException("Supplier not found with ID: " + request.getSupplierId()));

        if (!supplier.getIsActive()) {
            throw new DispatchException("Cannot create dispatch for inactive supplier: " + supplier.getName());
        }

        // â”€â”€â”€â”€ GATE 1: PO Approval Check â”€â”€â”€â”€
        PurchaseOrder po = null;
        List<PurchaseOrderItem> poItems = null;

        if (request.getPoId() != null) {
            po = purchaseOrderRepository.findById(request.getPoId())
                    .orElseThrow(() -> new DispatchException(
                            "Purchase Order not found with ID: " + request.getPoId()));

            // Only APPROVED or PAID POs can be dispatched
            if (!"APPROVED".equals(po.getStatus()) && !"PAID".equals(po.getStatus())
                    && !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
                throw new DispatchException(
                        "Cannot dispatch against PO #" + po.getPoNo()
                                + ". PO status is '" + po.getStatus()
                                + "' â€” only APPROVED or PAID POs can be dispatched.");
            }

            // Validate supplier match
            if (!po.getSupplierId().equals(request.getSupplierId())) {
                throw new DispatchException(
                        "Supplier mismatch: PO supplier (ID:" + po.getSupplierId()
                                + ") does not match dispatch supplier (ID:" + request.getSupplierId() + ")");
            }

            // Validate branch match
            if (!po.getBranchId().equals(request.getBranchId())) {
                throw new DispatchException(
                        "Branch mismatch: PO branch (ID:" + po.getBranchId()
                                + ") does not match dispatch branch (ID:" + request.getBranchId() + ")");
            }

            poItems = poItemRepository.findByPoId(request.getPoId());
            if (poItems.isEmpty()) {
                throw new DispatchException("PO #" + po.getPoNo() + " has no items.");
            }
        }

        // â”€â”€â”€â”€ GATE 2â€“5: Per-Item Validation â”€â”€â”€â”€
        // Aggregate quantities per product across all dispatch lines (handles IMEI 1-per-line)
        Map<Long, BigDecimal> dispatchQtyByProduct = new HashMap<>();
        for (DispatchCreateRequestDTO.DispatchItemCreateDTO itemDto : request.getItems()) {
            dispatchQtyByProduct.merge(
                    itemDto.getProductId(),
                    itemDto.getQtyDispatched(),
                    BigDecimal::add);
        }

        // Collect all serial numbers from ALL items for cross-item duplicate check
        Set<String> allSerialsInDispatch = new HashSet<>();

        for (DispatchCreateRequestDTO.DispatchItemCreateDTO itemDto : request.getItems()) {
            // â”€â”€ GATE 6: Product must exist and be active â”€â”€
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new DispatchException(
                            "Product not found with ID: " + itemDto.getProductId()));

            if (!product.getIsActive()) {
                throw new DispatchException(
                        "Product '" + product.getName() + "' is inactive and cannot be dispatched.");
            }

            // â”€â”€ GATE 2: Item must be in PO â”€â”€
            if (po != null && poItems != null) {
                PurchaseOrderItem poItem = findPoItemByProduct(poItems, itemDto.getProductId());
                if (poItem == null) {
                    throw new DispatchException(
                            "Product '" + product.getName()
                                    + "' (ID:" + product.getProductId()
                                    + ") is NOT listed in PO #" + po.getPoNo()
                                    + ". Only PO items can be dispatched.");
                }

                // â”€â”€ GATE 3: Quantity limit (check aggregated qty per product) â”€â”€
                BigDecimal totalDispatchQty = dispatchQtyByProduct.get(itemDto.getProductId());
                BigDecimal remaining = poItem.getQtyOrdered().subtract(poItem.getQtyDispatched());
                if (totalDispatchQty.compareTo(remaining) > 0) {
                    throw new DispatchException(
                            "Cannot dispatch " + totalDispatchQty.stripTrailingZeros().toPlainString()
                                    + " of '" + product.getName()
                                    + "'. PO remaining: " + remaining.stripTrailingZeros().toPlainString()
                                    + " (Ordered: " + poItem.getQtyOrdered().stripTrailingZeros().toPlainString()
                                    + ", Already received: " + poItem.getQtyDispatched().stripTrailingZeros().toPlainString() + ")");
                }
            }

            // â”€â”€ GATE 4: IMEI/Serial validation (for serialized products) â”€â”€
            if (Boolean.TRUE.equals(product.getIsSerialized())) {
                validateIMEIs(itemDto, product, allSerialsInDispatch);
            }

            // â”€â”€ GATE 5: Expiry/FEFO validation â”€â”€
            if (itemDto.getExpiryDate() != null) {
                if (itemDto.getExpiryDate().isBefore(LocalDate.now())) {
                    throw new DispatchException(
                            "Cannot dispatch expired batch for '" + product.getName()
                                    + "' (expiry: " + itemDto.getExpiryDate() + ")");
                }
                if (itemDto.getBatchCode() == null || itemDto.getBatchCode().trim().isEmpty()) {
                    throw new DispatchException(
                            "Batch code is required for expiry-tracked item: " + product.getName());
                }
            }
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // All gates passed â€” proceed with dispatch creation
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

        // Generate dispatch number
        String dispatchNo = generateDispatchNumber(request.getBranchId(), request.getPoId(), supplier.getCode());

        // Create dispatch record
        Dispatch dispatch = new Dispatch();
        dispatch.setDispatchNo(dispatchNo);
        dispatch.setBranchId(request.getBranchId());
        dispatch.setSupplierId(request.getSupplierId());
        dispatch.setPoId(request.getPoId());
        dispatch.setDispatchDate(request.getDispatchDate());
        dispatch.setInvoiceNo(request.getInvoiceNo());
        dispatch.setInvoiceDate(request.getInvoiceDate());
        dispatch.setCreatedBy(currentUserId);
        dispatch.setStatus("PENDING");
        dispatch.setPaymentStatus("UNPAID");

        // Calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DispatchCreateRequestDTO.DispatchItemCreateDTO itemDto : request.getItems()) {
            BigDecimal itemTotal = itemDto.getQtyDispatched().multiply(itemDto.getUnitPrice());
            totalAmount = totalAmount.add(itemTotal);
        }

        dispatch.setTotalAmount(totalAmount);
        dispatch.setNetAmount(totalAmount);

        // Save dispatch
        dispatch = dispatchRepository.save(dispatch);

        // Create dispatch line items
        for (DispatchCreateRequestDTO.DispatchItemCreateDTO itemDto : request.getItems()) {
            productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new DispatchException("Product not found with ID: " + itemDto.getProductId()));

            DispatchItem dispatchItem = new DispatchItem();
            dispatchItem.setDispatchId(dispatch.getDispatchId());
            dispatchItem.setProductId(itemDto.getProductId());
            dispatchItem.setToBranchId(dispatch.getBranchId());
            dispatchItem.setBatchId(null);
            dispatchItem.setBatchCode(itemDto.getBatchCode());
            dispatchItem.setExpiryDate(itemDto.getExpiryDate());
            dispatchItem.setQtyDispatched(itemDto.getQtyDispatched());
            dispatchItem.setUnitPrice(itemDto.getUnitPrice());
            dispatchItem.setSellingPrice(itemDto.getSellingPrice());
            dispatchItem.setMrp(itemDto.getMrp());
            dispatchItem.setItemType(
                    itemDto.getSerialNo() != null && !itemDto.getSerialNo().trim().isEmpty()
                            ? "imei"
                            : (itemDto.getExpiryDate() != null ? "expiry" : "normal"));
            dispatchItem.setSerialNo(itemDto.getSerialNo());
            dispatchItem.setTotal(itemDto.getQtyDispatched().multiply(itemDto.getUnitPrice()));

            dispatchItemRepository.save(dispatchItem);
        }

        log.info("Dispatch created successfully: {} (PO: {}, Items: {})",
                dispatchNo, request.getPoId(), request.getItems().size());
        return convertToResponseDTO(dispatch);
    }

    // ========================================================================
    //  DISPATCH APPROVAL â€” updates stock + PO qty_received + auto-closes PO
    // ========================================================================

    @Override
    public DispatchResponseDTO approveDispatch(Long dispatchId, Long approvedBy) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));

        if (!"PENDING".equals(dispatch.getStatus())) {
            throw new DispatchException("Can only approve dispatches in PENDING status. Current: " + dispatch.getStatus());
        }

        // Update stock levels
        updateStockFromDispatch(dispatch);

        // â”€â”€â”€â”€ NEW: Update PO item qty_dispatched â”€â”€â”€â”€
        if (dispatch.getPoId() != null) {
            updatePOReceivedQuantities(dispatch);
        }

        // Update dispatch status
        dispatch.setStatus("APPROVED");
        dispatch.setApprovedBy(approvedBy);
        dispatch.setApprovedAt(LocalDateTime.now());
        dispatch = dispatchRepository.save(dispatch);

        // Create payment request for the approved dispatch
        try {
            paymentRequestService.createPaymentRequest(dispatchId, approvedBy);
            log.info("Payment request created for dispatch: {}", dispatchId);
        } catch (Exception e) {
            log.warn("Failed to create payment request for dispatch {}: {}", dispatchId, e.getMessage());
        }

        log.info("Dispatch approved: {} by user: {}", dispatchId, approvedBy);
        return convertToResponseDTO(dispatch);
    }

    // ========================================================================
    //  PO QUANTITY TRACKING â€” auto-updates received quantities on PO items
    // ========================================================================

    private void updatePOReceivedQuantities(Dispatch dispatch) {
        List<DispatchItem> dispatchLines = dispatchItemRepository.findByDispatchId(dispatch.getDispatchId());
        List<PurchaseOrderItem> poItems = poItemRepository.findByPoId(dispatch.getPoId());

        // Aggregate dispatched quantities by product
        Map<Long, BigDecimal> dispatchedByProduct = new HashMap<>();
        for (DispatchItem line : dispatchLines) {
            dispatchedByProduct.merge(
                    line.getProductId(),
                    line.getQtyDispatched(),
                    BigDecimal::add);
        }

        // Update each PO item's qty_dispatched
        for (Map.Entry<Long, BigDecimal> entry : dispatchedByProduct.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal dispatchedQty = entry.getValue();

            PurchaseOrderItem poItem = poItems.stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst()
                    .orElse(null);

            if (poItem != null) {
                BigDecimal oldReceived = poItem.getQtyDispatched();
                BigDecimal newReceived = oldReceived.add(dispatchedQty);
                poItem.setQtyDispatched(newReceived);
                poItemRepository.save(poItem);

                log.info("PO item (product:{}) qty_dispatched updated: {} â†’ {} (dispatched: {})",
                        productId,
                        oldReceived.stripTrailingZeros().toPlainString(),
                        newReceived.stripTrailingZeros().toPlainString(),
                        dispatchedQty.stripTrailingZeros().toPlainString());
            }
        }

        // Refresh PO items after update to check completion
        poItems = poItemRepository.findByPoId(dispatch.getPoId());

        // Check if PO is fully received â†’ auto-close
        boolean allFullyReceived = poItems.stream().allMatch(item ->
                item.getQtyDispatched().compareTo(item.getQtyOrdered()) >= 0);

        boolean someReceived = poItems.stream().anyMatch(item ->
                item.getQtyDispatched().compareTo(BigDecimal.ZERO) > 0);

        PurchaseOrder po = purchaseOrderRepository.findById(dispatch.getPoId()).orElse(null);
        if (po != null) {
            if (allFullyReceived) {
                po.setStatus("FULLY_RECEIVED");
                purchaseOrderRepository.save(po);
                log.info("PO #{} is now FULLY_RECEIVED â€” all items dispatched", po.getPoNo());
            } else if (someReceived && !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
                po.setStatus("PARTIALLY_RECEIVED");
                purchaseOrderRepository.save(po);
                log.info("PO #{} is now PARTIALLY_RECEIVED", po.getPoNo());
            }
        }
    }

    // ========================================================================
    //  IMEI VALIDATION ENGINE
    // ========================================================================

    private void validateIMEIs(DispatchCreateRequestDTO.DispatchItemCreateDTO itemDto,
                               Product product,
                               Set<String> allSerialsInDispatch) {
        if (itemDto.getSerialNo() == null || itemDto.getSerialNo().trim().isEmpty()) {
            throw new DispatchException(
                    "IMEI/Serial number(s) required for serialized product: " + product.getName());
        }

        List<String> imeis = parseSerialTokens(itemDto.getSerialNo());

        // IMEI count must match qty
        int expectedCount = itemDto.getQtyDispatched().intValue();
        if (imeis.size() != expectedCount) {
            throw new DispatchException(
                    "IMEI count (" + imeis.size() + ") does not match dispatched quantity ("
                            + expectedCount + ") for " + product.getName());
        }

        for (String imei : imeis) {
            // No duplicates within this dispatch (cross-item check)
            if (!allSerialsInDispatch.add(imei)) {
                throw new DispatchException(
                        "Duplicate IMEI '" + imei + "' found within this dispatch for " + product.getName());
            }

            // No duplicates in system (existing IN_STOCK)
            Optional<ProductSerial> existing = productSerialRepository.findBySerialNo(imei);
            if (existing.isPresent() && "IN_STOCK".equals(existing.get().getStatus())) {
                throw new DispatchException(
                        "IMEI '" + imei + "' already exists in stock at branch "
                                + existing.get().getBranchId()
                                + ". Cannot dispatch a duplicate.");
            }
        }
    }

    // ========================================================================
    //  REMAINING CRUD OPERATIONS (unchanged logic, same as before)
    // ========================================================================

    @Override
    public DispatchResponseDTO getDispatchById(Long dispatchId) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));
        return convertToResponseDTO(dispatch);
    }

    @Override
    public List<DispatchResponseDTO> getDispatchesByBranch(Long branchId) {
        List<Dispatch> dispatches = dispatchRepository.findByBranchId(branchId);
        return dispatches.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DispatchResponseDTO> searchDispatches(DispatchFilterDTO filter) {
        List<Dispatch> dispatches = dispatchRepository.findByFilters(
                filter.getBranchId(),
                filter.getSupplierId(),
                filter.getStatus(),
                filter.getPaymentStatus(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getDispatchNo(),
                filter.getInvoiceNo()
        );

        return dispatches.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DispatchResponseDTO updateDispatch(Long dispatchId, DispatchUpdateRequestDTO request) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));

        if (!"PENDING".equals(dispatch.getStatus())) {
            throw new DispatchException("Cannot update dispatch that is not in PENDING status");
        }

        if (request.getInvoiceNo() != null) {
            dispatch.setInvoiceNo(request.getInvoiceNo());
        }
        if (request.getInvoiceDate() != null) {
            dispatch.setInvoiceDate(request.getInvoiceDate());
        }

        dispatch = dispatchRepository.save(dispatch);
        log.info("Dispatch updated: {}", dispatchId);

        return convertToResponseDTO(dispatch);
    }

    @Override
    public DispatchResponseDTO rejectDispatch(Long dispatchId, Long rejectedBy, String reason) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));

        if (!"PENDING".equals(dispatch.getStatus())) {
            throw new DispatchException("Can only reject dispatches in PENDING status");
        }

        dispatch.setStatus("REJECTED");
        dispatch.setApprovedBy(rejectedBy);
        dispatch = dispatchRepository.save(dispatch);

        log.info("Dispatch rejected: {} by user: {}", dispatchId, rejectedBy);
        return convertToResponseDTO(dispatch);
    }

    @Override
    public DispatchResponseDTO updatePaymentStatus(Long dispatchId, String paymentStatus) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));

        if (!"APPROVED".equals(dispatch.getStatus())) {
            throw new DispatchException("Can only update payment status for approved dispatches");
        }

        dispatch.setPaymentStatus(paymentStatus);
        dispatch = dispatchRepository.save(dispatch);

        log.info("Dispatch payment status updated: {} to {}", dispatchId, paymentStatus);
        return convertToResponseDTO(dispatch);
    }

    @Override
    public void deleteDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new DispatchException("Dispatch not found with ID: " + dispatchId));

        if (!"PENDING".equals(dispatch.getStatus())) {
            throw new DispatchException("Can only delete dispatches in PENDING status");
        }

        dispatchItemRepository.deleteAll(dispatchItemRepository.findByDispatchId(dispatchId));
        dispatchRepository.delete(dispatch);

        log.info("Dispatch deleted: {}", dispatchId);
    }

    @Override
    public DispatchStatsDTO getDispatchStats(Long branchId, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        startDate = switch (period != null ? period.toLowerCase() : "month") {
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };

        List<Dispatch> dispatches = dispatchRepository.findByBranchIdAndDispatchDateBetween(branchId, startDate, endDate);

        DispatchStatsDTO stats = new DispatchStatsDTO();
        stats.setPeriod(period);
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        stats.setTotalDispatches((long) dispatches.size());
        stats.setPendingDispatches(dispatches.stream().filter(g -> "PENDING".equals(g.getStatus())).count());
        stats.setApprovedDispatches(dispatches.stream().filter(g -> "APPROVED".equals(g.getStatus())).count());
        stats.setRejectedDispatches(dispatches.stream().filter(g -> "REJECTED".equals(g.getStatus())).count());

        BigDecimal totalValue = dispatches.stream()
                .filter(g -> "APPROVED".equals(g.getStatus()))
                .map(Dispatch::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalValue(totalValue);

        return stats;
    }

    @Override
    public List<DispatchItemDTO> getDispatchItemsByProduct(Long productId, Long branchId) {
        List<DispatchItem> items = dispatchItemRepository.findByBranchIdAndProductId(branchId, productId);
        return items.stream()
                .map(this::convertToDispatchItemDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isDispatchNumberExists(String dispatchNo) {
        return dispatchRepository.findByDispatchNo(dispatchNo).isPresent();
    }

    // ========================================================================
    //  HELPER METHODS
    // ========================================================================

    private PurchaseOrderItem findPoItemByProduct(List<PurchaseOrderItem> poItems, Long productId) {
        return poItems.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private synchronized String generateDispatchNumber(Long branchId, Long poId, String supplierCode) {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        long count = dispatchRepository.countByBranchIdAndDispatchDate(branchId, today);
        long sequence = count + 1;

        String branchCode = branchRepository.findById(branchId)
                .map(b -> normalizeToken(b.getCode(), 6, "B" + branchId))
                .orElse("B" + branchId);

        String normalizedSupplierCode = normalizeToken(supplierCode, 8, "SUP");
        String poRef = "GEN";
        if (poId != null) {
            poRef = purchaseOrderRepository.findById(poId)
                    .map(PurchaseOrder::getPoNo)
                    .map(poNo -> normalizeToken(poNo, 10, "PO" + poId))
                    .orElse("PO" + poId);
        }

        boolean dispatcherFlow = poId != null;
        String dispatchNo;
        do {
            String sequenceStr = String.format("%03d", sequence);
            dispatchNo = dispatcherFlow
                    ? String.format("DSP-%s-%s-%s-%s-%s", branchCode, poRef, normalizedSupplierCode, dateStr, sequenceStr)
                    : String.format("DSP-%s-%s-%s", branchCode, dateStr, sequenceStr);
            sequence++;
        } while (isDispatchNumberExists(dispatchNo));

        return dispatchNo;
    }

    private String normalizeToken(String value, int maxLength, String fallback) {
        String source = (value == null || value.trim().isEmpty()) ? fallback : value;
        String normalized = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.trim().isEmpty()) {
            normalized = fallback;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private void updateStockFromDispatch(Dispatch dispatch) {
        List<DispatchItem> items = dispatchItemRepository.findByDispatchId(dispatch.getDispatchId());

        for (DispatchItem item : items) {
            Optional<Stock> stockOpt = stockRepository.findByBranchIdAndProductId(dispatch.getBranchId(), item.getProductId());

            Stock stock;
            if (stockOpt.isPresent()) {
                stock = stockOpt.get();
                stock.setQuantity(stock.getQuantity().add(item.getQtyDispatched()));
                stock.setAvailableQty(stock.getAvailableQty().add(item.getQtyDispatched()));
            } else {
                stock = new Stock();
                stock.setBranchId(dispatch.getBranchId());
                stock.setProductId(item.getProductId());
                stock.setQuantity(item.getQtyDispatched());
                stock.setReservedQty(BigDecimal.ZERO);
                stock.setAvailableQty(item.getQtyDispatched());
            }

            stockRepository.save(stock);

            // Update product prices to latest dispatch prices
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setCostPrice(item.getUnitPrice());
                product.setSellingPrice(item.getSellingPrice());
                product.setMrp(item.getMrp());
                productRepository.save(product);
                log.info("Updated product {} prices: Cost={}, Selling={}, MRP={}",
                        product.getProductId(), item.getUnitPrice(), item.getSellingPrice(), item.getMrp());
            });

            Long createdBatchId = null;

            // Create batch if batch code is provided
            if (item.getBatchCode() != null && !item.getBatchCode().trim().isEmpty()) {
                Batch createdBatch = createBatch(dispatch.getBranchId(), item);
                createdBatchId = createdBatch.getBatchId();
            }

            upsertSerialsForDispatchedItem(dispatch, item, createdBatchId);
        }
    }

    private Batch createBatch(Long branchId, DispatchItem item) {
        Batch batch = new Batch();
        batch.setProductId(item.getProductId());
        batch.setBranchId(branchId);
        batch.setBatchCode(item.getBatchCode());
        batch.setExpiryDate(item.getExpiryDate());
        batch.setQty(item.getQtyDispatched());
        batch.setCostPrice(item.getUnitPrice());
        batch.setSellingPrice(item.getSellingPrice());
        batch.setMrp(item.getMrp());
        batch.setCreatedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    private void upsertSerialsForDispatchedItem(Dispatch dispatch, DispatchItem item, Long batchId) {
        if (item.getSerialNo() == null || item.getSerialNo().trim().isEmpty()) {
            return;
        }

        List<String> serialValues = parseSerialTokens(item.getSerialNo());
        for (String serialNo : serialValues) {
            Optional<ProductSerial> existing = productSerialRepository.findBySerialNo(serialNo);
            ProductSerial serial = existing.orElseGet(ProductSerial::new);

            serial.setProductId(item.getProductId());
            serial.setBranchId(dispatch.getBranchId());
            serial.setSerialNo(serialNo);
            serial.setBatchId(batchId);
            serial.setStatus("IN_STOCK");
            serial.setDispatchId(dispatch.getDispatchId());
            serial.setSaleId(null);
            serial.setSoldAt(null);

            productSerialRepository.save(serial);
        }
    }

    private List<String> parseSerialTokens(String rawSerials) {
        String normalized = rawSerials.replace("\n", ",").replace(";", ",");
        List<String> serials = new ArrayList<>();
        for (String token : normalized.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                serials.add(trimmed);
            }
        }
        return serials;
    }

    /**
     * Validates basic Dispatch item fields: productId, qty, unitPrice.
     */
    private void validateDispatchItems(List<DispatchCreateRequestDTO.DispatchItemCreateDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new DispatchException("Dispatch must have at least one item");
        }

        for (int i = 0; i < items.size(); i++) {
            DispatchCreateRequestDTO.DispatchItemCreateDTO item = items.get(i);

            if (item.getProductId() == null) {
                throw new DispatchException("Item " + (i + 1) + ": Product ID is required");
            }

            if (item.getQtyDispatched() == null) {
                throw new DispatchException("Item " + (i + 1) + ": Quantity cannot be null");
            }

            if (item.getQtyDispatched().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DispatchException("Item " + (i + 1) + ": Quantity must be greater than zero");
            }

            if (item.getUnitPrice() == null) {
                throw new DispatchException("Item " + (i + 1) + ": Unit price cannot be null");
            }

            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new DispatchException("Item " + (i + 1) + ": Unit price cannot be negative");
            }
        }
    }

    // ========================================================================
    //  DTO CONVERSION
    // ========================================================================

    private DispatchResponseDTO convertToResponseDTO(Dispatch dispatch) {
        DispatchResponseDTO dto = new DispatchResponseDTO();
        dto.setDispatchId(dispatch.getDispatchId());
        dto.setDispatchNo(dispatch.getDispatchNo());
        dto.setBranchId(dispatch.getBranchId());
        dto.setSupplierId(dispatch.getSupplierId());
        dto.setPoId(dispatch.getPoId());
        dto.setDispatchDate(dispatch.getDispatchDate());
        dto.setInvoiceNo(dispatch.getInvoiceNo());
        dto.setInvoiceDate(dispatch.getInvoiceDate());
        dto.setTotalAmount(dispatch.getTotalAmount());
        dto.setNetAmount(dispatch.getNetAmount());
        dto.setPaymentStatus(dispatch.getPaymentStatus());
        dto.setStatus(dispatch.getStatus());
        dto.setCreatedBy(dispatch.getCreatedBy());
        if (dispatch.getCreatedBy() != null) {
            userRepository.findById(dispatch.getCreatedBy()).ifPresent(u -> dto.setCreatedByName(u.getFullName()));
        }
        dto.setApprovedBy(dispatch.getApprovedBy());
        if (dispatch.getApprovedBy() != null) {
            userRepository.findById(dispatch.getApprovedBy()).ifPresent(u -> dto.setApprovedByName(u.getFullName()));
        }
        dto.setCreatedAt(dispatch.getCreatedAt());

        supplierRepository.findById(dispatch.getSupplierId()).ifPresent(supplier ->
                dto.setSupplierName(supplier.getName()));

        branchRepository.findById(dispatch.getBranchId()).ifPresent(branch ->
                dto.setBranchName(branch.getName()));

        List<DispatchItem> items = dispatchItemRepository.findByDispatchId(dispatch.getDispatchId());
        List<DispatchItemDTO> itemDTOs = items.stream()
                .map(this::convertToDispatchItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private DispatchItemDTO convertToDispatchItemDTO(DispatchItem item) {
        DispatchItemDTO dto = new DispatchItemDTO();
        dto.setDispatchItemId(item.getDispatchItemId());
        dto.setDispatchId(item.getDispatchId());
        dto.setProductId(item.getProductId());
        dto.setBatchCode(item.getBatchCode());
        dto.setExpiryDate(item.getExpiryDate());
        dto.setQtyDispatched(item.getQtyDispatched());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSellingPrice(item.getSellingPrice());
        dto.setMrp(item.getMrp());
        dto.setTotal(item.getTotal());

        productRepository.findById(item.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductSku(product.getSku());
        });

        return dto;
    }
}

