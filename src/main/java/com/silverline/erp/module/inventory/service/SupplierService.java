package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.SupplierRequestDTO;
import com.silverline.erp.module.inventory.dto.SupplierResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {

    SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO);

    SupplierResponseDTO updateSupplier(Long supplierId, SupplierRequestDTO requestDTO);

    void deleteSupplier(Long supplierId);

    SupplierResponseDTO getSupplierById(Long supplierId);

    Page<SupplierResponseDTO> getAllSuppliers(Pageable pageable);

    List<SupplierResponseDTO> getActiveSuppliers();
}

