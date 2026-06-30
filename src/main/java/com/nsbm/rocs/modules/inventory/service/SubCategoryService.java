package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.SubCategoryDTO;
import com.nsbm.rocs.entity.inventory.SubCategory;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.modules.inventory.repository.CategoryRepository;
import com.nsbm.rocs.modules.inventory.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    public List<SubCategoryDTO> getAllSubCategories() {
        return subCategoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SubCategoryDTO> getActiveSubCategories() {
        return subCategoryRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SubCategoryDTO> getSubCategoriesByCategoryId(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SubCategoryDTO getSubCategoryById(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        return convertToDTO(subCategory);
    }

    public SubCategoryDTO createSubCategory(SubCategoryDTO subCategoryDTO) {
        // Validate category exists
        categoryRepository.findById(subCategoryDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + subCategoryDTO.getCategoryId()));

        SubCategory subCategory = convertToEntity(subCategoryDTO);
        SubCategory savedSubCategory = subCategoryRepository.save(subCategory);
        return convertToDTO(savedSubCategory);
    }

    public SubCategoryDTO updateSubCategory(Long id, SubCategoryDTO subCategoryDTO) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        // Validate category exists if changed
        if (!subCategory.getCategoryId().equals(subCategoryDTO.getCategoryId())) {
            categoryRepository.findById(subCategoryDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + subCategoryDTO.getCategoryId()));
        }

        subCategory.setCategoryId(subCategoryDTO.getCategoryId());
        subCategory.setName(subCategoryDTO.getName());
        subCategory.setDescription(subCategoryDTO.getDescription());
        subCategory.setIsActive(subCategoryDTO.getIsActive());

        SubCategory updatedSubCategory = subCategoryRepository.save(subCategory);
        return convertToDTO(updatedSubCategory);
    }

    public void deleteSubCategory(Long id) {
        if (!subCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + id);
        }
        subCategoryRepository.deleteById(id);
    }

    public void deactivateSubCategory(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        subCategory.setIsActive(false);
        subCategoryRepository.save(subCategory);
    }

    private SubCategoryDTO convertToDTO(SubCategory subCategory) {
        SubCategoryDTO dto = new SubCategoryDTO();
        dto.setSubcategoryId(subCategory.getSubcategoryId());
        dto.setCategoryId(subCategory.getCategoryId());
        dto.setName(subCategory.getName());
        dto.setDescription(subCategory.getDescription());
        dto.setIsActive(subCategory.getIsActive());

        // Set category name for display
        categoryRepository.findById(subCategory.getCategoryId())
                .ifPresent(category -> dto.setCategoryName(category.getName()));

        return dto;
    }

    private SubCategory convertToEntity(SubCategoryDTO dto) {
        SubCategory subCategory = new SubCategory();
        subCategory.setSubcategoryId(dto.getSubcategoryId());
        subCategory.setCategoryId(dto.getCategoryId());
        subCategory.setName(dto.getName());
        subCategory.setDescription(dto.getDescription());
        subCategory.setIsActive(dto.getIsActive());
        return subCategory;
    }
}


