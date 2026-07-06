package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.module.inventory.dto.ProductSerialDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductSerialService {
    List<ProductSerialDTO> getAllSerials();

    List<ProductSerialDTO> getSerialsByProduct(Long productId);

    List<ProductSerialDTO> getSerialsByBranch(Long branchId);

    List<ProductSerialDTO> getSerialsByStatus(String status);

    List<ProductSerialDTO> getAvailableSerials(Long branchId, Long productId);

    Page<ProductSerialDTO> lookupSerials(
            Long branchId,
            Long productId,
            List<Long> branchIds,
            List<Long> productIds,
            String status,
            String search,
            int page,
            int size);

    ProductSerialDTO getSerialById(Long id);

    ProductSerialDTO getSerialBySerialNo(String serialNo);

    ProductSerialDTO createSerial(ProductSerialDTO serialDTO);

    List<ProductSerialDTO> createBulkSerials(List<ProductSerialDTO> serialDTOs);

    ProductSerialDTO updateSerial(Long id, ProductSerialDTO serialDTO);

    ProductSerialDTO markAsSold(Long serialId, Long saleId);

    ProductSerialDTO markAsDamaged(Long serialId);

    ProductSerialDTO markAsReturned(Long serialId);

    void deleteSerial(Long id);

    List<DamagedProductDTO> getDamagedProducts(Long branchId);

    ProductSerialDTO findSerialBySerialNo(String serialNo);
}
