package com.nsbm.rocs.modules.inventory.service.impl;

import com.nsbm.rocs.entity.inventory.Supplier;
import com.nsbm.rocs.entity.inventory.SupplierBranch;
import com.nsbm.rocs.entity.inventory.SupplierBranchId;
import com.nsbm.rocs.entity.inventory.SupplierContact;
import com.nsbm.rocs.modules.inventory.dto.SupplierBranchDTO;
import com.nsbm.rocs.modules.inventory.dto.SupplierContactDTO;
import com.nsbm.rocs.modules.inventory.dto.SupplierRequestDTO;
import com.nsbm.rocs.modules.inventory.dto.SupplierResponseDTO;
import com.nsbm.rocs.common.exception.DuplicateResourceException;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.modules.inventory.repository.SupplierRepository;
import com.nsbm.rocs.modules.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO) {
        validateDuplicateCode(requestDTO.getCode(), null);
        Supplier supplier = new Supplier();
        mapBasicFields(supplier, requestDTO);

        // Save supplier first to get the ID
        Supplier savedSupplier = supplierRepository.save(supplier);

        // Now handle contacts and branches with the saved supplier ID
        handleContactsAndBranches(savedSupplier, requestDTO);

        // Save again with contacts and branches
        savedSupplier = supplierRepository.save(savedSupplier);
        return mapToResponse(savedSupplier);
    }

    @Override
    public SupplierResponseDTO updateSupplier(Long supplierId, SupplierRequestDTO requestDTO) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        validateDuplicateCode(requestDTO.getCode(), supplierId);

        // Clear existing relationships
        supplier.getContacts().clear();
        supplier.getBranches().clear();

        // Update basic fields
        mapBasicFields(supplier, requestDTO);

        // Handle contacts and branches
        handleContactsAndBranches(supplier, requestDTO);

        Supplier updated = supplierRepository.save(supplier);
        return mapToResponse(updated);
    }

    @Override
    public void deleteSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        supplierRepository.delete(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierById(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        return mapToResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active suppliers
     */
    public List<SupplierResponseDTO> getActiveSuppliers() {
        return supplierRepository.findAll().stream()
                .filter(supplier -> supplier.getIsActive() != null && supplier.getIsActive())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateDuplicateCode(String code, Long existingId) {
        supplierRepository.findByCode(code).ifPresent(existing -> {
            boolean isDuplicate = existingId == null || !existingId.equals(existing.getSupplierId());
            if (isDuplicate) {
                throw new DuplicateResourceException("Supplier with code " + code + " already exists");
            }
        });
    }

    private void mapBasicFields(Supplier supplier, SupplierRequestDTO dto) {
        supplier.setCode(dto.getCode());
        supplier.setName(dto.getName());
        supplier.setCompanyName(dto.getCompanyName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setMobile(dto.getMobile());
        supplier.setEmail(dto.getEmail());
        supplier.setWebsite(dto.getWebsite());
        supplier.setAddressLine1(dto.getAddressLine1());
        supplier.setAddressLine2(dto.getAddressLine2());
        supplier.setCity(dto.getCity());
        supplier.setCountry(dto.getCountry());
        supplier.setTaxId(dto.getTaxId());
        supplier.setCreditDays(dto.getCreditDays());
        supplier.setCreditLimit(dto.getCreditLimit());
        supplier.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        supplier.setCreatedBy(dto.getCreatedBy());
    }

    private void handleContactsAndBranches(Supplier supplier, SupplierRequestDTO dto) {
        // Handle contacts
        if (dto.getContacts() != null && !dto.getContacts().isEmpty()) {
            dto.getContacts().forEach(contactDTO -> {
                SupplierContact contact = new SupplierContact();
                contact.setSupplier(supplier);
                contact.setName(contactDTO.getName());
                contact.setDesignation(contactDTO.getDesignation());
                contact.setPhone(contactDTO.getPhone());
                contact.setEmail(contactDTO.getEmail());
                contact.setIsPrimary(contactDTO.getIsPrimary() != null ? contactDTO.getIsPrimary() : false);
                supplier.getContacts().add(contact);
            });
        }

        // Handle branches
        if (dto.getBranches() != null && !dto.getBranches().isEmpty()) {
            dto.getBranches().forEach(branchDTO -> {
                SupplierBranch branch = new SupplierBranch();
                SupplierBranchId id = new SupplierBranchId();
                id.setSupplierId(supplier.getSupplierId()); // Now this will have a value
                id.setBranchId(branchDTO.getBranchId());
                branch.setId(id);
                branch.setSupplier(supplier);
                branch.setIsPreferred(branchDTO.getIsPreferred() != null ? branchDTO.getIsPreferred() : false);
                branch.setDiscountPercentage(branchDTO.getDiscountPercentage());
                branch.setNotes(branchDTO.getNotes());
                supplier.getBranches().add(branch);
            });
        }
    }

    private SupplierResponseDTO mapToResponse(Supplier supplier) {
        SupplierResponseDTO dto = new SupplierResponseDTO();
        dto.setSupplierId(supplier.getSupplierId());
        dto.setCode(supplier.getCode());
        dto.setName(supplier.getName());
        dto.setCompanyName(supplier.getCompanyName());
        dto.setContactPerson(supplier.getContactPerson());
        dto.setPhone(supplier.getPhone());
        dto.setMobile(supplier.getMobile());
        dto.setEmail(supplier.getEmail());
        dto.setWebsite(supplier.getWebsite());
        dto.setAddressLine1(supplier.getAddressLine1());
        dto.setAddressLine2(supplier.getAddressLine2());
        dto.setCity(supplier.getCity());
        dto.setCountry(supplier.getCountry());
        dto.setTaxId(supplier.getTaxId());
        dto.setCreditDays(supplier.getCreditDays());
        dto.setCreditLimit(supplier.getCreditLimit());
        dto.setIsActive(supplier.getIsActive());
        dto.setCreatedBy(supplier.getCreatedBy());
        dto.setCreatedAt(supplier.getCreatedAt());
        dto.setUpdatedAt(supplier.getUpdatedAt());

        // Handle contacts with null safety
        if (supplier.getContacts() != null) {
            dto.setContacts(supplier.getContacts().stream()
                    .map(contact -> {
                        SupplierContactDTO contactDTO = new SupplierContactDTO();
                        contactDTO.setContactId(contact.getContactId());
                        contactDTO.setName(contact.getName());
                        contactDTO.setDesignation(contact.getDesignation());
                        contactDTO.setPhone(contact.getPhone());
                        contactDTO.setEmail(contact.getEmail());
                        contactDTO.setIsPrimary(contact.getIsPrimary());
                        return contactDTO;
                    }).collect(Collectors.toList()));
        } else {
            dto.setContacts(new ArrayList<>());
        }

        // Handle branches with null safety
        if (supplier.getBranches() != null) {
            dto.setBranches(supplier.getBranches().stream()
                    .map(branch -> {
                        SupplierBranchDTO branchDTO = new SupplierBranchDTO();
                        branchDTO.setBranchId(branch.getId() != null ? branch.getId().getBranchId() : null);
                        branchDTO.setIsPreferred(branch.getIsPreferred());
                        branchDTO.setDiscountPercentage(branch.getDiscountPercentage());
                        branchDTO.setNotes(branch.getNotes());
                        return branchDTO;
                    }).collect(Collectors.toList()));
        } else {
            dto.setBranches(new ArrayList<>());
        }

        return dto;
    }
}


