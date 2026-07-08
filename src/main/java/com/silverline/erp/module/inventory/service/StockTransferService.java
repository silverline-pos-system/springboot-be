package com.silverline.erp.module.inventory.service;

import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.inventory.ProductSerial;
import com.silverline.erp.domain.inventory.StockTransfer;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.inventory.dto.StockTransferRequestDTO;
import com.silverline.erp.module.inventory.dto.StockTransferResponseDTO;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.ProductSerialRepository;
import com.silverline.erp.module.inventory.repository.StockTransferRepository;
import com.silverline.erp.module.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final UserProfileRepository userProfileRepository;
    private final StockService stockService;
    private final ProductSerialRepository productSerialRepository;
    private final BatchRepository batchRepository;

    @Value("${rocs.imei.suffix-length:9}")
    private int suffixLength;

    private int getSuffixLength() {
        return suffixLength;
    }

    private String generateTransferNo() {
        String today = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRF-" + today;
        long count = stockTransferRepository.count();
        return String.format("%s-%05d", prefix, count + 1);
    }

    private ProductSerial findSerialEntityByScan(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        String cleanQuery = query.trim();
        Optional<ProductSerial> fullMatch = productSerialRepository.findBySerialNo(cleanQuery);
        if (fullMatch.isPresent()) {
            return fullMatch.get();
        }

        int suffixLen = getSuffixLength();
        if (cleanQuery.length() >= suffixLen) {
            String suffix = cleanQuery.substring(cleanQuery.length() - suffixLen);
            List<ProductSerial> suffixMatches = productSerialRepository.findBySerialNoSuffix(suffix);
            if (suffixMatches.isEmpty()) {
                return null;
            }
            if (suffixMatches.size() > 1) {
                throw new com.silverline.erp.common.exception.ValidationException(
                        "Multiple matching IMEI records found for suffix '" + suffix + "'. Please resolve the duplicate data."
                );
            }
            return suffixMatches.get(0);
        }
        return null;
    }

    private void deductBatchQty(Batch batch, java.math.BigDecimal qtyToDeduct) {
        java.math.BigDecimal currentQty = batch.getQty() != null ? batch.getQty() : java.math.BigDecimal.ZERO;
        if (currentQty.compareTo(qtyToDeduct) < 0) {
            throw new com.silverline.erp.common.exception.ValidationException("Insufficient quantity in batch: " + batch.getBatchCode());
        }
        batch.setQty(currentQty.subtract(qtyToDeduct));
        batchRepository.save(batch);
    }

    private Batch findOrCreateDestBatch(Batch sourceBatch, Long destBranchId, java.math.BigDecimal qtyToAdd) {
        List<Batch> destCandidates = batchRepository.findAllByBranchIdAndProductIdAndBatchCodeOrdered(
                destBranchId, sourceBatch.getProductId(), sourceBatch.getBatchCode()
        );

        Batch destBatch;
        if (!destCandidates.isEmpty()) {
            destBatch = destCandidates.get(0);
            java.math.BigDecimal currentQty = destBatch.getQty() != null ? destBatch.getQty() : java.math.BigDecimal.ZERO;
            destBatch.setQty(currentQty.add(qtyToAdd));
        } else {
            destBatch = new Batch();
            destBatch.setBatchCode(sourceBatch.getBatchCode());
            destBatch.setProductId(sourceBatch.getProductId());
            destBatch.setBranchId(destBranchId);
            destBatch.setCostPrice(sourceBatch.getCostPrice());
            destBatch.setSellingPrice(sourceBatch.getSellingPrice());
            destBatch.setMrp(sourceBatch.getMrp());
            destBatch.setExpiryDate(sourceBatch.getExpiryDate());
            destBatch.setManufacturingDate(sourceBatch.getManufacturingDate());
            destBatch.setQty(qtyToAdd);
            destBatch.setCreatedAt(java.time.LocalDateTime.now());
        }
        return batchRepository.save(destBatch);
    }

    private StockTransferResponseDTO convertToResponseDTO(StockTransfer transfer) {
        StockTransferResponseDTO dto = new StockTransferResponseDTO();
        dto.setTransferId(transfer.getId());
        dto.setTransferNo(transfer.getTransferNo());
        dto.setFromBranchId(transfer.getFromBranch().getBranchId());
        dto.setFromBranchName(transfer.getFromBranch().getName());
        dto.setToBranchId(transfer.getToBranch().getBranchId());
        dto.setToBranchName(transfer.getToBranch().getName());
        dto.setProductId(transfer.getProduct().getProductId());
        dto.setProductName(transfer.getProduct().getName());
        if (transfer.getBatch() != null) {
            dto.setBatchId(transfer.getBatch().getBatchId());
            dto.setBatchCode(transfer.getBatch().getBatchCode());
        }
        dto.setQuantity(transfer.getQuantity().intValue());
        dto.setTransferDate(transfer.getTransferDate().toLocalDate());
        dto.setRemarks(transfer.getRemarks());
        dto.setTransferStatus(transfer.getTransferStatus());
        dto.setRequestedBy(transfer.getRequestedBy().getUserId());
        dto.setRequestedByName(transfer.getRequestedBy().getUsername());
        dto.setRequestedTime(transfer.getRequestedTime());
        if (transfer.getApprovedBy() != null) {
            dto.setApprovedBy(transfer.getApprovedBy().getUserId());
            dto.setApprovedTime(transfer.getApprovedTime());
        }

        if (Boolean.TRUE.equals(transfer.getProduct().getIsSerialized())) {
            List<String> imeis = productSerialRepository.findByTransferId(transfer.getId()).stream()
                    .map(ProductSerial::getSerialNo)
                    .collect(Collectors.toList());
            dto.setImeis(imeis);
        } else {
            dto.setImeis(List.of());
        }

        return dto;
    }

    @Transactional
    public StockTransferResponseDTO createTransfer(StockTransferRequestDTO request) {
        Branch fromBranch = branchRepository.findById(request.getFromBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Source branch not found: " + request.getFromBranchId()));
        Branch toBranch = branchRepository.findById(request.getToBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination branch not found: " + request.getToBranchId()));
        if (fromBranch.getBranchId().equals(toBranch.getBranchId())) {
            throw new com.silverline.erp.common.exception.ValidationException("Source and destination branches must be different");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        UserProfile requestedBy = userProfileRepository.findById(request.getRequestedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getRequestedBy()));

        Batch batch = null;
        if (request.getBatchId() != null) {
            batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNo(generateTransferNo());
        transfer.setFromBranch(fromBranch);
        transfer.setToBranch(toBranch);
        transfer.setProduct(product);
        transfer.setBatch(batch);
        transfer.setQuantity(java.math.BigDecimal.valueOf(request.getQuantity()));
        transfer.setTransferDate(request.getTransferDate() != null ? request.getTransferDate().atStartOfDay() : java.time.LocalDateTime.now());
        transfer.setRemarks(request.getRemarks());

        String status = request.getTransferStatus() != null ? request.getTransferStatus().toUpperCase() : "DRAFT";
        if (!status.equals("DRAFT") && !status.equals("PENDING")) {
            throw new com.silverline.erp.common.exception.ValidationException("Initial transfer status must be either DRAFT or PENDING");
        }
        transfer.setTransferStatus(status);
        transfer.setRequestedBy(requestedBy);
        transfer.setRequestedTime(java.time.LocalDateTime.now());

        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        if (Boolean.TRUE.equals(product.getIsSerialized())) {
            if (request.getImeis() == null || request.getImeis().isEmpty()) {
                throw new com.silverline.erp.common.exception.ValidationException("IMEI list is required for serialized products");
            }
            if (request.getImeis().size() != request.getQuantity()) {
                throw new com.silverline.erp.common.exception.ValidationException("Number of selected IMEIs (" + request.getImeis().size() + ") must equal transfer quantity (" + request.getQuantity() + ")");
            }
            long distinctCount = request.getImeis().stream().distinct().count();
            if (distinctCount != request.getImeis().size()) {
                throw new com.silverline.erp.common.exception.ValidationException("Duplicate IMEIs selected in request");
            }

            for (String imeiQuery : request.getImeis()) {
                ProductSerial serial = findSerialEntityByScan(imeiQuery);
                if (serial == null) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI not found in system: " + imeiQuery);
                }
                if (!"IN_STOCK".equals(serial.getStatus())) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " is not IN_STOCK (Status: " + serial.getStatus() + ")");
                }
                if (!serial.getBranchId().equals(fromBranch.getBranchId())) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " does not belong to source branch " + fromBranch.getName());
                }
                if (serial.getTransferId() != null) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " is already in another transfer request");
                }
                serial.setTransferId(savedTransfer.getId());
                productSerialRepository.save(serial);
            }
        }

        if ("PENDING".equals(status)) {
            if (!Boolean.TRUE.equals(product.getIsSerialized())) {
                if (!stockService.isStockAvailable(fromBranch.getBranchId(), product.getProductId(), request.getQuantity())) {
                    throw new com.silverline.erp.common.exception.ValidationException("Insufficient stock at source branch: " + fromBranch.getName());
                }
                stockService.reserveStock(fromBranch.getBranchId(), product.getProductId(), request.getQuantity());
            }
        }

        return convertToResponseDTO(savedTransfer);
    }

    public List<StockTransferResponseDTO> getTransfers(String status, Long fromBranchId, Long toBranchId, Long requestedBy) {
        return stockTransferRepository.findAll().stream()
                .filter(t -> status == null || t.getTransferStatus().equalsIgnoreCase(status))
                .filter(t -> fromBranchId == null || t.getFromBranch().getBranchId().equals(fromBranchId))
                .filter(t -> toBranchId == null || t.getToBranch().getBranchId().equals(toBranchId))
                .filter(t -> requestedBy == null || t.getRequestedBy().getUserId().equals(requestedBy))
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public StockTransferResponseDTO getTransferById(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));
        return convertToResponseDTO(transfer);
    }

    @Transactional
    public StockTransferResponseDTO updateTransfer(Long id, StockTransferRequestDTO request) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));

        if (!"DRAFT".equals(transfer.getTransferStatus())) {
            throw new com.silverline.erp.common.exception.ValidationException("Only DRAFT transfers can be modified");
        }

        Branch fromBranch = branchRepository.findById(request.getFromBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Source branch not found: " + request.getFromBranchId()));
        Branch toBranch = branchRepository.findById(request.getToBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination branch not found: " + request.getToBranchId()));
        if (fromBranch.getBranchId().equals(toBranch.getBranchId())) {
            throw new com.silverline.erp.common.exception.ValidationException("Source and destination branches must be different");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        Batch batch = null;
        if (request.getBatchId() != null) {
            batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));
        }

        List<ProductSerial> oldSerials = productSerialRepository.findByTransferId(id);
        for (ProductSerial s : oldSerials) {
            s.setTransferId(null);
            productSerialRepository.save(s);
        }

        transfer.setFromBranch(fromBranch);
        transfer.setToBranch(toBranch);
        transfer.setProduct(product);
        transfer.setBatch(batch);
        transfer.setQuantity(java.math.BigDecimal.valueOf(request.getQuantity()));
        if (request.getTransferDate() != null) {
            transfer.setTransferDate(request.getTransferDate().atStartOfDay());
        }
        transfer.setRemarks(request.getRemarks());

        String newStatus = request.getTransferStatus() != null ? request.getTransferStatus().toUpperCase() : "DRAFT";
        transfer.setTransferStatus(newStatus);

        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        if (Boolean.TRUE.equals(product.getIsSerialized())) {
            if (request.getImeis() == null || request.getImeis().isEmpty()) {
                throw new com.silverline.erp.common.exception.ValidationException("IMEI list is required for serialized products");
            }
            if (request.getImeis().size() != request.getQuantity()) {
                throw new com.silverline.erp.common.exception.ValidationException("Number of selected IMEIs must equal transfer quantity");
            }
            long distinctCount = request.getImeis().stream().distinct().count();
            if (distinctCount != request.getImeis().size()) {
                throw new com.silverline.erp.common.exception.ValidationException("Duplicate IMEIs selected in request");
            }

            for (String imeiQuery : request.getImeis()) {
                ProductSerial serial = findSerialEntityByScan(imeiQuery);
                if (serial == null) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI not found: " + imeiQuery);
                }
                if (!"IN_STOCK".equals(serial.getStatus())) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " is not IN_STOCK");
                }
                if (!serial.getBranchId().equals(fromBranch.getBranchId())) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " does not belong to source branch");
                }
                if (serial.getTransferId() != null) {
                    throw new com.silverline.erp.common.exception.ValidationException("IMEI " + serial.getSerialNo() + " is already in another transfer request");
                }
                serial.setTransferId(savedTransfer.getId());
                productSerialRepository.save(serial);
            }
        }

        if ("PENDING".equals(newStatus)) {
            if (!Boolean.TRUE.equals(product.getIsSerialized())) {
                if (!stockService.isStockAvailable(fromBranch.getBranchId(), product.getProductId(), request.getQuantity())) {
                    throw new com.silverline.erp.common.exception.ValidationException("Insufficient stock at source branch: " + fromBranch.getName());
                }
                stockService.reserveStock(fromBranch.getBranchId(), product.getProductId(), request.getQuantity());
            }
        }

        return convertToResponseDTO(savedTransfer);
    }

    @Transactional
    public String submitTransfer(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));

        if (!"DRAFT".equals(transfer.getTransferStatus())) {
            throw new com.silverline.erp.common.exception.ValidationException("Only DRAFT transfers can be submitted");
        }

        transfer.setTransferStatus("PENDING");
        stockTransferRepository.save(transfer);

        if (!Boolean.TRUE.equals(transfer.getProduct().getIsSerialized())) {
            int qty = transfer.getQuantity().intValue();
            if (!stockService.isStockAvailable(transfer.getFromBranch().getBranchId(), transfer.getProduct().getProductId(), qty)) {
                throw new com.silverline.erp.common.exception.ValidationException("Insufficient stock at source branch");
            }
            stockService.reserveStock(transfer.getFromBranch().getBranchId(), transfer.getProduct().getProductId(), qty);
        }

        return "Transfer request submitted successfully";
    }

    @Transactional
    public String approveTransfer(Long id, String approvalNotes) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));

        if (!"PENDING".equals(transfer.getTransferStatus())) {
            throw new com.silverline.erp.common.exception.ValidationException("Only PENDING transfers can be approved");
        }

        transfer.setApprovedBy(transfer.getRequestedBy());
        transfer.setApprovedTime(java.time.LocalDateTime.now());
        transfer.setTransferStatus("APPROVED");
        if (approvalNotes != null) {
            transfer.setRemarks(transfer.getRemarks() != null ? transfer.getRemarks() + " | Approve: " + approvalNotes : "Approve: " + approvalNotes);
        }

        Long fromBranchId = transfer.getFromBranch().getBranchId();
        Long toBranchId = transfer.getToBranch().getBranchId();
        Long productId = transfer.getProduct().getProductId();
        int quantity = transfer.getQuantity().intValue();

        if (Boolean.TRUE.equals(transfer.getProduct().getIsSerialized())) {
            List<ProductSerial> serials = productSerialRepository.findByTransferId(id);
            if (serials.size() != quantity) {
                throw new com.silverline.erp.common.exception.ValidationException(
                        "Found mismatch between transfer quantity (" + quantity + ") and associated IMEIs (" + serials.size() + ")"
                );
            }

            for (ProductSerial serial : serials) {
                serial.setBranchId(toBranchId);
                serial.setTransferId(null);

                if (serial.getBatchId() != null) {
                    Batch sourceBatch = batchRepository.findById(serial.getBatchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Source batch not found: " + serial.getBatchId()));

                    deductBatchQty(sourceBatch, java.math.BigDecimal.ONE);

                    Batch destBatch = findOrCreateDestBatch(sourceBatch, toBranchId, java.math.BigDecimal.ONE);
                    serial.setBatchId(destBatch.getBatchId());
                }

                productSerialRepository.save(serial);
            }

            stockService.reduceStock(fromBranchId, productId, quantity);
            stockService.increaseStock(toBranchId, productId, quantity);

        } else {
            stockService.releaseReservedStock(fromBranchId, productId, quantity);
            stockService.reduceStock(fromBranchId, productId, quantity);
            stockService.increaseStock(toBranchId, productId, quantity);

            if (transfer.getBatch() != null) {
                Batch sourceBatch = transfer.getBatch();
                deductBatchQty(sourceBatch, transfer.getQuantity());

                findOrCreateDestBatch(sourceBatch, toBranchId, transfer.getQuantity());
            }
        }

        stockTransferRepository.save(transfer);
        return "Transfer request approved successfully";
    }

    @Transactional
    public String rejectTransfer(Long id, String rejectionReason) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));

        if (!"PENDING".equals(transfer.getTransferStatus())) {
            throw new com.silverline.erp.common.exception.ValidationException("Only PENDING transfers can be rejected");
        }

        transfer.setTransferStatus("REJECTED");
        if (rejectionReason != null) {
            transfer.setRemarks(transfer.getRemarks() != null ? transfer.getRemarks() + " | Reject: " + rejectionReason : "Reject: " + rejectionReason);
        }

        if (!Boolean.TRUE.equals(transfer.getProduct().getIsSerialized())) {
            stockService.releaseReservedStock(
                    transfer.getFromBranch().getBranchId(),
                    transfer.getProduct().getProductId(),
                    transfer.getQuantity().intValue()
            );
        } else {
            List<ProductSerial> serials = productSerialRepository.findByTransferId(id);
            for (ProductSerial serial : serials) {
                serial.setTransferId(null);
                productSerialRepository.save(serial);
            }
        }

        stockTransferRepository.save(transfer);
        return "Transfer request rejected successfully";
    }

    @Transactional
    public String deleteTransfer(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + id));

        if (!"DRAFT".equals(transfer.getTransferStatus())) {
            throw new com.silverline.erp.common.exception.ValidationException("Only DRAFT transfers can be deleted");
        }

        List<ProductSerial> serials = productSerialRepository.findByTransferId(id);
        for (ProductSerial serial : serials) {
            serial.setTransferId(null);
            productSerialRepository.save(serial);
        }

        stockTransferRepository.delete(transfer);
        return "Transfer request deleted successfully";
    }
}
