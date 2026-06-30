package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.DamagedProductDTO;

import java.util.List;

public interface DamageService {

    List<DamagedProductDTO> getAllDamages(Long branchId, Long productId);

    DamagedProductDTO createDamage(DamagedProductDTO damageDTO);
}


