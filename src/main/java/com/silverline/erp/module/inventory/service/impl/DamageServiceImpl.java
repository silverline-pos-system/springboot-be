package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.domain.inventory.ProductSerial;
import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.ProductSerialRepository;
import com.silverline.erp.module.inventory.service.DamageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DamageServiceImpl implements DamageService {

    private final ProductSerialRepository productSerialRepository;
    private final ProductRepository productRepository;
    private final List<DamagedProductDTO> damageHistory = new ArrayList<>(); // In-memory storage for demo

    @Override
    @Transactional(readOnly = true)
    public List<DamagedProductDTO> getAllDamages(Long branchId, Long productId) {
        return damageHistory.stream()
                .filter(damage -> branchId == null || damage.getBranchId().equals(branchId))
                .filter(damage -> productId == null || damage.getProductId().equals(productId))
                .toList();
    }

    @Override
    public DamagedProductDTO createDamage(DamagedProductDTO damageDTO) {
        // If serial ID is provided, update the serial status
        if (damageDTO.getSerialId() != null) {
            ProductSerial serial = productSerialRepository.findById(damageDTO.getSerialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial not found with id: " + damageDTO.getSerialId()));

            serial.setStatus("DAMAGED");
            productSerialRepository.save(serial);

            // Populate DTO with serial details
            damageDTO.setSerialNo(serial.getSerialNo());
            damageDTO.setProductId(serial.getProductId());
            damageDTO.setBranchId(serial.getBranchId());
        }

        // Validate product exists
        if (damageDTO.getProductId() != null) {
            productRepository.findById(damageDTO.getProductId()).ifPresent(product -> {
                damageDTO.setProductName(product.getName());
                damageDTO.setProductSku(product.getSku());
            });
        }

        // Store in history
        damageDTO.setMessage("Damage entry recorded successfully");
        damageHistory.add(damageDTO);

        return damageDTO;
    }
}


