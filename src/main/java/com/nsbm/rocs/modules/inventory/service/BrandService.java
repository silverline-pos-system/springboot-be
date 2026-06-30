package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.BrandDTO;
import com.nsbm.rocs.entity.inventory.Brand;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.modules.inventory.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BrandDTO> getActiveBrands() {
        return brandRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BrandDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return convertToDTO(brand);
    }

    public BrandDTO createBrand(BrandDTO brandDTO) {
        Brand brand = convertToEntity(brandDTO);
        Brand savedBrand = brandRepository.save(brand);
        return convertToDTO(savedBrand);
    }

    public BrandDTO updateBrand(Long id, BrandDTO brandDTO) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        brand.setName(brandDTO.getName());
        brand.setDescription(brandDTO.getDescription());
        brand.setIsActive(brandDTO.getIsActive());

        Brand updatedBrand = brandRepository.save(brand);
        return convertToDTO(updatedBrand);
    }

    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }

    public void deactivateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    private BrandDTO convertToDTO(Brand brand) {
        BrandDTO dto = new BrandDTO();
        dto.setBrandId(brand.getBrandId());
        dto.setName(brand.getName());
        dto.setDescription(brand.getDescription());
        dto.setIsActive(brand.getIsActive());
        return dto;
    }

    private Brand convertToEntity(BrandDTO dto) {
        Brand brand = new Brand();
        brand.setBrandId(dto.getBrandId());
        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setIsActive(dto.getIsActive());
        return brand;
    }
}


