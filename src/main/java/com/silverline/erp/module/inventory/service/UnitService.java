package com.silverline.erp.module.inventory.service;

import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.product.Unit;
import com.silverline.erp.module.inventory.dto.UnitDTO;
import com.silverline.erp.module.inventory.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UnitService {

    private final UnitRepository unitRepository;

    @Cacheable(value = "units")
    public List<UnitDTO> getAllUnits() {
        return unitRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "units", key = "#id")
    public UnitDTO getUnitById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        return convertToDTO(unit);
    }

    @CacheEvict(value = "units", allEntries = true)
    public UnitDTO createUnit(UnitDTO unitDTO) {
        Unit unit = convertToEntity(unitDTO);
        Unit savedUnit = unitRepository.save(unit);
        return convertToDTO(savedUnit);
    }

    @CacheEvict(value = "units", allEntries = true)
    public UnitDTO updateUnit(Long id, UnitDTO unitDTO) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));

        unit.setName(unitDTO.getName());
        unit.setSymbol(unitDTO.getSymbol());

        Unit updatedUnit = unitRepository.save(unit);
        return convertToDTO(updatedUnit);
    }

    @CacheEvict(value = "units", allEntries = true)
    public void deleteUnit(Long id) {
        if (!unitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Unit not found with id: " + id);
        }
        unitRepository.deleteById(id);
    }

    private UnitDTO convertToDTO(Unit unit) {
        UnitDTO dto = new UnitDTO();
        dto.setUnitId(unit.getUnitId());
        dto.setName(unit.getName());
        dto.setSymbol(unit.getSymbol());
        return dto;
    }

    private Unit convertToEntity(UnitDTO dto) {
        Unit unit = new Unit();
        unit.setUnitId(dto.getUnitId());
        unit.setName(dto.getName());
        unit.setSymbol(dto.getSymbol());
        return unit;
    }
}


