package com.silverline.erp.module.procurement.service.impl;

import com.silverline.erp.common.event.GrnReceivedEvent;
import com.silverline.erp.common.exception.GrnException;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.inventory.BranchProduct;
import com.silverline.erp.domain.inventory.ProductSerial;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.procurement.*;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.inventory.repository.*;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.module.procurement.repository.GrnItemRepository;
import com.silverline.erp.module.procurement.repository.GrnRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderItemRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderRepository;
import com.silverline.erp.module.procurement.service.GrnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Goods Received Note service. Receives a supplier delivery into a single branch
 * against a PO. Ported from the old dispatch flow, with two changes: goods go to
 * the GRN's own branch (no per-line branch), and pricing is written to the
 * per-branch price list (branch_product), never to the global product.
 */
@Service("grnService")
@Transactional
@Slf4j
@RequiredArgsConstructor
public class GrnServiceImpl implements GrnService {

    private final GrnRepository grnRepository;
    private final GrnItemRepository grnItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final BatchRepository batchRepository;
    private final ProductSerialRepository productSerialRepository;
    private final BranchProductRepository branchProductRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ========================================================================
    //  GRN CREATION (DRAFT) - with full PO + IMEI + FEFO validation gates
    // ========================================================================

    @Override
    public GrnResponseDTO createGrn(GrnCreateRequestDTO request, Long currentUserId) {
        log.info("Creating GRN for branch: {}, supplier: {}, poId: {}",
                request.getBranchId(), request.getSupplierId(), request.getPoId());

        validateGrnItems(request.getItems());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new GrnException("Supplier not found with ID: " + request.getSupplierId()));

        if (!supplier.getIsActive()) {
            throw new GrnException("Cannot create GRN for inactive supplier: " + supplier.getName());
        }

        PurchaseOrder po = null;
        List<PurchaseOrderItem> poItems = null;

        if (request.getPoId() != null) {
            po = purchaseOrderRepository.findById(request.getPoId())
                    .orElseThrow(() -> new GrnException("Purchase Order not found with ID: " + request.getPoId()));

            if (!"APPROVED".equals(po.getStatus()) && !"PAID".equals(po.getStatus())
                    && !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
                throw new GrnException("Cannot receive against PO #" + po.getPoNo()
                        + ". PO status is '" + po.getStatus()
                        + "' - only APPROVED, PAID or PARTIALLY_RECEIVED POs can be received.");
            }

            if (!po.getSupplierId().equals(request.getSupplierId())) {
                throw new GrnException("Supplier mismatch: PO supplier (ID:" + po.getSupplierId()
                        + ") does not match GRN supplier (ID:" + request.getSupplierId() + ")");
            }

            if (!po.getBranchId().equals(request.getBranchId())) {
                throw new GrnException("Branch mismatch: PO branch (ID:" + po.getBranchId()
                        + ") does not match GRN branch (ID:" + request.getBranchId() + ")");
            }

            poItems = poItemRepository.findByPoId(request.getPoId());
            if (poItems.isEmpty()) {
                throw new GrnException("PO #" + po.getPoNo() + " has no items.");
            }
        }

        // Aggregate quantities per product across all lines (handles IMEI 1-per-line)
        Map<Long, BigDecimal> receivedQtyByProduct = new HashMap<>();
        for (GrnCreateRequestDTO.GrnItemCreateDTO itemDto : request.getItems()) {
            receivedQtyByProduct.merge(itemDto.getProductId(), itemDto.getQtyReceived(), BigDecimal::add);
        }

        Set<String> allSerialsInGrn = new HashSet<>();

        for (GrnCreateRequestDTO.GrnItemCreateDTO itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new GrnException("Product not found with ID: " + itemDto.getProductId()));

            if (!product.getIsActive()) {
                throw new GrnException("Product '" + product.getName() + "' is inactive and cannot be received.");
            }

            if (po != null && poItems != null) {
                PurchaseOrderItem poItem = findPoItemByProduct(poItems, itemDto.getProductId());
                if (poItem == null) {
                    throw new GrnException("Product '" + product.getName()
                            + "' (ID:" + product.getProductId()
                            + ") is NOT listed in PO #" + po.getPoNo()
                            + ". Only PO items can be received.");
                }

                BigDecimal totalReceiveQty = receivedQtyByProduct.get(itemDto.getProductId());
                BigDecimal remaining = poItem.getQtyOrdered().subtract(poItem.getQtyDispatched());
                if (totalReceiveQty.compareTo(remaining) > 0) {
                    throw new GrnException("Cannot receive " + totalReceiveQty.stripTrailingZeros().toPlainString()
                            + " of '" + product.getName()
                            + "'. PO remaining: " + remaining.stripTrailingZeros().toPlainString()
                            + " (Ordered: " + poItem.getQtyOrdered().stripTrailingZeros().toPlainString()
                            + ", Already received: " + poItem.getQtyDispatched().stripTrailingZeros().toPlainString() + ")");
                }
            }

            if (Boolean.TRUE.equals(product.getIsSerialized())) {
                validateIMEIs(itemDto, product, allSerialsInGrn);
            }

            if (itemDto.getExpiryDate() != null) {
                if (itemDto.getExpiryDate().isBefore(LocalDate.now())) {
                    throw new GrnException("Cannot receive expired batch for '" + product.getName()
                            + "' (expiry: " + itemDto.getExpiryDate() + ")");
                }
                if (itemDto.getBatchCode() == null || itemDto.getBatchCode().trim().isEmpty()) {
                    throw new GrnException("Batch code is required for expiry-tracked item: " + product.getName());
                }
            }
        }

        String grnNo = generateGrnNumber(request.getBranchId(), request.getPoId(), supplier.getCode());

        Grn grn = new Grn();
        grn.setGrnNo(grnNo);
        grn.setBranchId(request.getBranchId());
        grn.setSupplierId(request.getSupplierId());
        grn.setPoId(request.getPoId());
        grn.setGrnDate(request.getGrnDate());
        grn.setInvoiceNo(request.getInvoiceNo());
        grn.setInvoiceDate(request.getInvoiceDate());
        grn.setReceivedBy(currentUserId);
        grn.setStatus("DRAFT");
        grn.setPaymentStatus("UNPAID");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (GrnCreateRequestDTO.GrnItemCreateDTO itemDto : request.getItems()) {
            totalAmount = totalAmount.add(itemDto.getQtyReceived().multiply(itemDto.getUnitPrice()));
        }
        grn.setTotalAmount(totalAmount);
        grn.setNetAmount(totalAmount);

        grn = grnRepository.save(grn);

        for (GrnCreateRequestDTO.GrnItemCreateDTO itemDto : request.getItems()) {
            GrnItem item = new GrnItem();
            item.setGrnId(grn.getGrnId());
            item.setProductId(itemDto.getProductId());
            item.setBatchId(null);
            item.setBatchCode(itemDto.getBatchCode());
            item.setExpiryDate(itemDto.getExpiryDate());
            item.setQtyReceived(itemDto.getQtyReceived());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setSellingPrice(itemDto.getSellingPrice());
            item.setMrp(itemDto.getMrp());
            item.setItemType(
                    itemDto.getSerialNo() != null && !itemDto.getSerialNo().trim().isEmpty()
                            ? "imei"
                            : (itemDto.getExpiryDate() != null ? "expiry" : "normal"));
            item.setSerialNo(itemDto.getSerialNo());
            item.setTotal(itemDto.getQtyReceived().multiply(itemDto.getUnitPrice()));
            grnItemRepository.save(item);
        }

        log.info("GRN created: {} (PO: {}, Items: {})", grnNo, request.getPoId(), request.getItems().size());
        return convertToResponseDTO(grn);
    }

    // ========================================================================
    //  GRN POSTING - updates stock + per-branch price + PO qty received
    // ========================================================================

    @Override
    public GrnResponseDTO postGrn(Long grnId, Long postedBy) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new GrnException("GRN not found with ID: " + grnId));

        if (!"DRAFT".equals(grn.getStatus())) {
            throw new GrnException("Can only post GRNs in DRAFT status. Current: " + grn.getStatus());
        }

        updateStockFromGrn(grn);

        if (grn.getPoId() != null) {
            updatePOReceivedQuantities(grn);
        }

        grn.setStatus("POSTED");
        grn.setPostedBy(postedBy);
        grn.setPostedAt(LocalDateTime.now());
        grn = grnRepository.save(grn);

        // Payment request is created after this transaction commits (AFTER_COMMIT listener).
        eventPublisher.publishEvent(new GrnReceivedEvent(
                grn.getGrnId(), grn.getGrnNo(), grn.getBranchId(), grn.getPoId(), postedBy));

        log.info("GRN posted: {} by user: {}", grnId, postedBy);
        return convertToResponseDTO(grn);
    }

    private void updatePOReceivedQuantities(Grn grn) {
        List<GrnItem> lines = grnItemRepository.findByGrnId(grn.getGrnId());
        List<PurchaseOrderItem> poItems = poItemRepository.findByPoId(grn.getPoId());

        Map<Long, BigDecimal> receivedByProduct = new HashMap<>();
        for (GrnItem line : lines) {
            receivedByProduct.merge(line.getProductId(), line.getQtyReceived(), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : receivedByProduct.entrySet()) {
            PurchaseOrderItem poItem = poItems.stream()
                    .filter(i -> i.getProductId().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (poItem != null) {
                poItem.setQtyDispatched(poItem.getQtyDispatched().add(entry.getValue()));
                poItemRepository.save(poItem);
            }
        }

        poItems = poItemRepository.findByPoId(grn.getPoId());
        boolean allFullyReceived = poItems.stream()
                .allMatch(item -> item.getQtyDispatched().compareTo(item.getQtyOrdered()) >= 0);
        boolean someReceived = poItems.stream()
                .anyMatch(item -> item.getQtyDispatched().compareTo(BigDecimal.ZERO) > 0);

        PurchaseOrder po = purchaseOrderRepository.findById(grn.getPoId()).orElse(null);
        if (po != null) {
            if (allFullyReceived) {
                po.setStatus("FULLY_RECEIVED");
                purchaseOrderRepository.save(po);
                log.info("PO #{} is now FULLY_RECEIVED", po.getPoNo());
            } else if (someReceived && !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
                po.setStatus("PARTIALLY_RECEIVED");
                purchaseOrderRepository.save(po);
                log.info("PO #{} is now PARTIALLY_RECEIVED", po.getPoNo());
            }
        }
    }

    private void validateIMEIs(GrnCreateRequestDTO.GrnItemCreateDTO itemDto, Product product, Set<String> allSerialsInGrn) {
        if (itemDto.getSerialNo() == null || itemDto.getSerialNo().trim().isEmpty()) {
            throw new GrnException("IMEI/Serial number(s) required for serialized product: " + product.getName());
        }

        List<String> imeis = parseSerialTokens(itemDto.getSerialNo());
        int expectedCount = itemDto.getQtyReceived().intValue();
        if (imeis.size() != expectedCount) {
            throw new GrnException("IMEI count (" + imeis.size() + ") does not match received quantity ("
                    + expectedCount + ") for " + product.getName());
        }

        for (String imei : imeis) {
            if (!allSerialsInGrn.add(imei)) {
                throw new GrnException("Duplicate IMEI '" + imei + "' found within this GRN for " + product.getName());
            }
            Optional<ProductSerial> existing = productSerialRepository.findBySerialNo(imei);
            if (existing.isPresent() && "IN_STOCK".equals(existing.get().getStatus())) {
                throw new GrnException("IMEI '" + imei + "' already exists in stock at branch "
                        + existing.get().getBranchId() + ". Cannot receive a duplicate.");
            }
        }
    }

    // ========================================================================
    //  READ / CRUD
    // ========================================================================

    @Override
    public GrnResponseDTO getGrnById(Long grnId) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new GrnException("GRN not found with ID: " + grnId));
        return convertToResponseDTO(grn);
    }

    @Override
    public List<GrnResponseDTO> getGrnsByBranch(Long branchId) {
        return grnRepository.findByBranchId(branchId).stream()
                .sorted(Comparator.comparing(Grn::getGrnDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GrnResponseDTO> searchGrns(GrnFilterDTO filter) {
        return grnRepository.findByFilters(
                        filter.getBranchId(), filter.getSupplierId(), filter.getStatus(),
                        filter.getPaymentStatus(), filter.getStartDate(), filter.getEndDate(),
                        filter.getGrnNo(), filter.getInvoiceNo())
                .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public GrnResponseDTO updatePaymentStatus(Long grnId, String paymentStatus) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new GrnException("GRN not found with ID: " + grnId));
        if (!"POSTED".equals(grn.getStatus())) {
            throw new GrnException("Can only update payment status for posted GRNs");
        }
        grn.setPaymentStatus(paymentStatus);
        grn = grnRepository.save(grn);
        return convertToResponseDTO(grn);
    }

    @Override
    public void deleteGrn(Long grnId) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new GrnException("GRN not found with ID: " + grnId));
        if (!"DRAFT".equals(grn.getStatus())) {
            throw new GrnException("Can only delete GRNs in DRAFT status");
        }
        grnItemRepository.deleteAll(grnItemRepository.findByGrnId(grnId));
        grnRepository.delete(grn);
        log.info("GRN deleted: {}", grnId);
    }

    @Override
    public GrnResponseDTO cancelGrn(Long grnId, Long cancelledBy, String reason) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new GrnException("GRN not found with ID: " + grnId));
        if (!"DRAFT".equals(grn.getStatus())) {
            throw new GrnException("Can only cancel GRNs in DRAFT status");
        }
        grn.setStatus("CANCELLED");
        grn.setPostedBy(cancelledBy);
        grn = grnRepository.save(grn);
        log.info("GRN cancelled: {} by user: {}", grnId, cancelledBy);
        return convertToResponseDTO(grn);
    }

    @Override
    public List<GrnItemDTO> getGrnItemsByProduct(Long productId, Long branchId) {
        return grnItemRepository.findByBranchIdAndProductId(branchId, productId).stream()
                .map(this::convertToGrnItemDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isGrnNumberExists(String grnNo) {
        return grnRepository.findByGrnNo(grnNo).isPresent();
    }

    // ========================================================================
    //  HELPERS
    // ========================================================================

    private PurchaseOrderItem findPoItemByProduct(List<PurchaseOrderItem> poItems, Long productId) {
        return poItems.stream().filter(i -> i.getProductId().equals(productId)).findFirst().orElse(null);
    }

    private synchronized String generateGrnNumber(Long branchId, Long poId, String supplierCode) {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = grnRepository.countByBranchIdAndGrnDate(branchId, today);
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

        String grnNo;
        do {
            String sequenceStr = String.format("%03d", sequence);
            grnNo = poId != null
                    ? String.format("GRN-%s-%s-%s-%s-%s", branchCode, poRef, normalizedSupplierCode, dateStr, sequenceStr)
                    : String.format("GRN-%s-%s-%s", branchCode, dateStr, sequenceStr);
            sequence++;
        } while (isGrnNumberExists(grnNo));

        return grnNo;
    }

    private String normalizeToken(String value, int maxLength, String fallback) {
        String source = (value == null || value.trim().isEmpty()) ? fallback : value;
        String normalized = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.trim().isEmpty()) {
            normalized = fallback;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private void updateStockFromGrn(Grn grn) {
        List<GrnItem> items = grnItemRepository.findByGrnId(grn.getGrnId());

        for (GrnItem item : items) {
            Optional<Stock> stockOpt = stockRepository.findByBranchIdAndProductId(grn.getBranchId(), item.getProductId());
            Stock stock;
            if (stockOpt.isPresent()) {
                stock = stockOpt.get();
                stock.setQuantity(stock.getQuantity().add(item.getQtyReceived()));
                stock.setAvailableQty(stock.getAvailableQty().add(item.getQtyReceived()));
            } else {
                stock = new Stock();
                stock.setBranchId(grn.getBranchId());
                stock.setProductId(item.getProductId());
                stock.setQuantity(item.getQtyReceived());
                stock.setReservedQty(BigDecimal.ZERO);
                stock.setAvailableQty(item.getQtyReceived());
            }
            stockRepository.save(stock);

            // Per-branch price (decision 5): the canonical price lives on branch_product.
            // POS pricing reads branch_product, so the global product price is no
            // longer written here.
            upsertBranchProduct(grn.getBranchId(), item, grn.getReceivedBy());

            Long createdBatchId = null;
            if (item.getBatchCode() != null && !item.getBatchCode().trim().isEmpty()) {
                createdBatchId = createBatch(grn.getBranchId(), item).getBatchId();
            }

            upsertSerialsForReceivedItem(grn, item, createdBatchId);
        }
    }

    private void upsertBranchProduct(Long branchId, GrnItem item, Long userId) {
        BranchProduct bp = branchProductRepository.findByBranchIdAndProductId(branchId, item.getProductId())
                .orElseGet(() -> {
                    BranchProduct created = new BranchProduct();
                    created.setBranchId(branchId);
                    created.setProductId(item.getProductId());
                    created.setAddedByBranchId(branchId);
                    created.setAddedByUserId(userId);
                    return created;
                });

        if (item.getUnitPrice() != null) bp.setCostPrice(item.getUnitPrice());
        if (item.getSellingPrice() != null) bp.setSellingPrice(item.getSellingPrice());
        if (item.getMrp() != null) bp.setMrp(item.getMrp());
        bp.setIsActive(true);
        branchProductRepository.save(bp);
    }

    private Batch createBatch(Long branchId, GrnItem item) {
        Batch batch = new Batch();
        batch.setProductId(item.getProductId());
        batch.setBranchId(branchId);
        batch.setBatchCode(item.getBatchCode());
        batch.setExpiryDate(item.getExpiryDate());
        batch.setQty(item.getQtyReceived());
        batch.setCostPrice(item.getUnitPrice());
        batch.setSellingPrice(item.getSellingPrice());
        batch.setMrp(item.getMrp());
        batch.setCreatedAt(LocalDateTime.now());
        return batchRepository.save(batch);
    }

    private void upsertSerialsForReceivedItem(Grn grn, GrnItem item, Long batchId) {
        if (item.getSerialNo() == null || item.getSerialNo().trim().isEmpty()) {
            return;
        }
        for (String serialNo : parseSerialTokens(item.getSerialNo())) {
            ProductSerial serial = productSerialRepository.findBySerialNo(serialNo).orElseGet(ProductSerial::new);
            serial.setProductId(item.getProductId());
            serial.setBranchId(grn.getBranchId());
            serial.setSerialNo(serialNo);
            serial.setBatchId(batchId);
            serial.setStatus("IN_STOCK");
            serial.setGrnId(grn.getGrnId());
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

    private void validateGrnItems(List<GrnCreateRequestDTO.GrnItemCreateDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new GrnException("GRN must have at least one item");
        }
        for (int i = 0; i < items.size(); i++) {
            GrnCreateRequestDTO.GrnItemCreateDTO item = items.get(i);
            if (item.getProductId() == null) {
                throw new GrnException("Item " + (i + 1) + ": Product ID is required");
            }
            if (item.getQtyReceived() == null) {
                throw new GrnException("Item " + (i + 1) + ": Quantity cannot be null");
            }
            if (item.getQtyReceived().compareTo(BigDecimal.ZERO) <= 0) {
                throw new GrnException("Item " + (i + 1) + ": Quantity must be greater than zero");
            }
            if (item.getUnitPrice() == null) {
                throw new GrnException("Item " + (i + 1) + ": Unit price cannot be null");
            }
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new GrnException("Item " + (i + 1) + ": Unit price cannot be negative");
            }
        }
    }

    // ========================================================================
    //  DTO CONVERSION
    // ========================================================================

    private GrnResponseDTO convertToResponseDTO(Grn grn) {
        GrnResponseDTO dto = new GrnResponseDTO();
        dto.setGrnId(grn.getGrnId());
        dto.setGrnNo(grn.getGrnNo());
        dto.setBranchId(grn.getBranchId());
        dto.setSupplierId(grn.getSupplierId());
        dto.setPoId(grn.getPoId());
        dto.setGrnDate(grn.getGrnDate());
        dto.setInvoiceNo(grn.getInvoiceNo());
        dto.setInvoiceDate(grn.getInvoiceDate());
        dto.setTotalAmount(grn.getTotalAmount());
        dto.setNetAmount(grn.getNetAmount());
        dto.setPaymentStatus(grn.getPaymentStatus());
        dto.setStatus(grn.getStatus());
        dto.setReceivedBy(grn.getReceivedBy());
        if (grn.getReceivedBy() != null) {
            userRepository.findById(grn.getReceivedBy()).ifPresent(u -> dto.setReceivedByName(u.getFullName()));
        }
        dto.setPostedBy(grn.getPostedBy());
        if (grn.getPostedBy() != null) {
            userRepository.findById(grn.getPostedBy()).ifPresent(u -> dto.setPostedByName(u.getFullName()));
        }
        dto.setCreatedAt(grn.getCreatedAt());

        supplierRepository.findById(grn.getSupplierId()).ifPresent(s -> dto.setSupplierName(s.getName()));
        branchRepository.findById(grn.getBranchId()).ifPresent(b -> dto.setBranchName(b.getName()));
        if (grn.getPoId() != null) {
            purchaseOrderRepository.findById(grn.getPoId()).ifPresent(p -> dto.setPoNo(p.getPoNo()));
        }

        List<GrnItemDTO> itemDTOs = grnItemRepository.findByGrnId(grn.getGrnId()).stream()
                .map(this::convertToGrnItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private GrnItemDTO convertToGrnItemDTO(GrnItem item) {
        GrnItemDTO dto = new GrnItemDTO();
        dto.setGrnItemId(item.getGrnItemId());
        dto.setGrnId(item.getGrnId());
        dto.setProductId(item.getProductId());
        dto.setBatchCode(item.getBatchCode());
        dto.setExpiryDate(item.getExpiryDate());
        dto.setQtyReceived(item.getQtyReceived());
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
