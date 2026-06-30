package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.SupplierRequestDTO;
import com.silverline.erp.module.inventory.dto.SupplierResponseDTO;

import java.util.List;

public interface SupplierService {

    SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO);

    SupplierResponseDTO updateSupplier(Long supplierId, SupplierRequestDTO requestDTO);

    void deleteSupplier(Long supplierId);

    SupplierResponseDTO getSupplierById(Long supplierId);

    List<SupplierResponseDTO> getAllSuppliers();

    List<SupplierResponseDTO> getActiveSuppliers();
}

