package com.silverline.erp.module.admin.service;

import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.system.PrintHeaderFooterSetting;
import com.silverline.erp.module.admin.dto.PrintHeaderFooterDTO;
import com.silverline.erp.module.admin.repository.PrintHeaderFooterSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrintSettingsService {

    private final PrintHeaderFooterSettingRepository repository;

    /**
     * Get header/footer settings for a branch.
     * Returns an empty/default DTO when no override exists for the branch.
     */
    public PrintHeaderFooterDTO getHeaderFooter(Long branchId) {
        return repository.findByBranchId(branchId)
                .map(this::toDTO)
                .orElseGet(this::emptyDTO);
    }

    /**
     * Create or update header/footer settings for a branch (upsert).
     */
    @Transactional
    public PrintHeaderFooterDTO saveHeaderFooter(Long branchId, PrintHeaderFooterDTO dto) {
        PrintHeaderFooterSetting setting = repository.findByBranchId(branchId)
                .orElseGet(() -> {
                    PrintHeaderFooterSetting s = new PrintHeaderFooterSetting();
                    s.setBranchId(branchId);
                    return s;
                });

        // Map header fields
        if (dto.getHeader() != null) {
            setting.setHeaderBusinessName(dto.getHeader().getBusinessName());
            setting.setHeaderBranchLine(dto.getHeader().getBranchLine());
            setting.setHeaderAddress(dto.getHeader().getAddress());
            setting.setHeaderContact(dto.getHeader().getContact());
            setting.setHeaderExtraLine(dto.getHeader().getExtraLine());
        }

        // Map footer fields
        if (dto.getFooter() != null) {
            setting.setFooterThankYouLine(dto.getFooter().getThankYouLine());
            setting.setFooterPolicyLine(dto.getFooter().getPolicyLine());
            setting.setFooterPoweredByLine(dto.getFooter().getPoweredByLine());
            setting.setFooterExtraLine(dto.getFooter().getExtraLine());
        }

        PrintHeaderFooterSetting saved = repository.save(setting);
        return toDTO(saved);
    }

    /**
     * Remove the branch override (hard delete).
     */
    @Transactional
    public void deleteHeaderFooter(Long branchId) {
        PrintHeaderFooterSetting setting = repository.findByBranchId(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No print settings found for branch " + branchId));
        repository.delete(setting);
    }

    // ===== Mapping helpers =====

    private PrintHeaderFooterDTO toDTO(PrintHeaderFooterSetting entity) {
        PrintHeaderFooterDTO dto = new PrintHeaderFooterDTO();

        PrintHeaderFooterDTO.HeaderDTO header = new PrintHeaderFooterDTO.HeaderDTO();
        header.setBusinessName(entity.getHeaderBusinessName());
        header.setBranchLine(entity.getHeaderBranchLine());
        header.setAddress(entity.getHeaderAddress());
        header.setContact(entity.getHeaderContact());
        header.setExtraLine(entity.getHeaderExtraLine());
        dto.setHeader(header);

        PrintHeaderFooterDTO.FooterDTO footer = new PrintHeaderFooterDTO.FooterDTO();
        footer.setThankYouLine(entity.getFooterThankYouLine());
        footer.setPolicyLine(entity.getFooterPolicyLine());
        footer.setPoweredByLine(entity.getFooterPoweredByLine());
        footer.setExtraLine(entity.getFooterExtraLine());
        dto.setFooter(footer);

        return dto;
    }

    private PrintHeaderFooterDTO emptyDTO() {
        PrintHeaderFooterDTO dto = new PrintHeaderFooterDTO();
        dto.setHeader(new PrintHeaderFooterDTO.HeaderDTO("", "", "", "", ""));
        dto.setFooter(new PrintHeaderFooterDTO.FooterDTO("", "", "", ""));
        return dto;
    }
}


