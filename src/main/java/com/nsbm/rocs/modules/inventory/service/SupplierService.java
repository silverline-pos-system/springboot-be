package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.SupplierRequestDTO;
import com.nsbm.rocs.modules.inventory.dto.SupplierResponseDTO;

import java.util.List;

public interface SupplierService {

    SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO);

    SupplierResponseDTO updateSupplier(Long supplierId, SupplierRequestDTO requestDTO);

    void deleteSupplier(Long supplierId);

    SupplierResponseDTO getSupplierById(Long supplierId);

    List<SupplierResponseDTO> getAllSuppliers();

    List<SupplierResponseDTO> getActiveSuppliers();
}

