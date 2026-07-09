package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.SaleRepository;
import com.silverline.erp.module.admin.service.BranchService;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;

    // Create
    @CacheEvict(value = "branches", allEntries = true)
    @Override
    @org.springframework.transaction.annotation.Transactional
    public BranchDTO createBranch(BranchDTO dto) {
        // Auto-generate next branch code on the backend to avoid concurrent client conflicts
        dto.setCode(generateNextBranchCode());

        if (dto.getName() != null && branchRepository.existsByName(dto.getName())) {
            throw new com.silverline.erp.common.exception.DuplicateResourceException("Branch name already exists");
        }
        if (dto.getCode() != null && branchRepository.existsByCode(dto.getCode())) {
            throw new com.silverline.erp.common.exception.DuplicateResourceException("Branch code already exists");
        }
        Branch entity = toEntity(dto);
        Branch saved = branchRepository.save(entity);
        return toDTO(saved);
    }

    // Read all
    @Cacheable(value = "branches")
    @Override
    public List<BranchDTO> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Read by id
    @Cacheable(value = "branches", key = "#id")
    @Override
    public BranchDTO getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        return toDTO(branch);
    }

    // Update
    @CacheEvict(value = "branches", allEntries = true)
    @Override
    @org.springframework.transaction.annotation.Transactional
    public BranchDTO updateBranch(Long id, BranchDTO dto) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        if (dto.getName() != null && !dto.getName().equals(existing.getName()) && branchRepository.existsByNameAndBranchIdNot(dto.getName(), id)) {
            throw new com.silverline.erp.common.exception.DuplicateResourceException("Branch name already exists");
        }
        if (dto.getCode() != null && !dto.getCode().equals(existing.getCode()) && branchRepository.existsByCodeAndBranchIdNot(dto.getCode(), id)) {
            throw new com.silverline.erp.common.exception.DuplicateResourceException("Branch code already exists");
        }

        // update fields
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getCode() != null) existing.setCode(dto.getCode());
        if (dto.getAddress() != null) existing.setAddress(dto.getAddress());
        if (dto.getLocation() != null) existing.setLocation(dto.getLocation());
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getIsActive() != null) existing.setIsActive(dto.getIsActive());

        Branch saved = branchRepository.save(existing);
        return toDTO(saved);
    }

    // Delete
    @CacheEvict(value = "branches", allEntries = true)
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteBranch(Long id) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branchRepository.delete(existing);
    }

    @CacheEvict(value = "branches", allEntries = true)
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void toggleBranchStatus(Long id) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (existing.getIsActive() == null) {
            existing.setIsActive(true);
        } else {
            existing.setIsActive(!existing.getIsActive());
        }
        branchRepository.save(existing);
    }

    @Override
    public Map<String, Object> getBranchSummary(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        BranchDTO branchDTO = toDTO(branch);

        // NOTE: No branch-specific users since user_profiles.branch_id is removed
        // Instead show all employees count
        long totalUsers = userRepository.count();

        Map<String, Object> response = new HashMap<>();
        response.put("branch", branchDTO);
        response.put("userCount", totalUsers);

        return response;
    }

    @Override
    public Map<String, Object> getBranchRealTimeSales(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch not found");
        }

        Map<String, Object> response = new HashMap<>();

        try {
            BigDecimal sales = saleRepository.getDailySales(id, LocalDate.now());
            response.put("dailySales", sales != null ? sales.doubleValue() : 0.0);
        } catch (Exception e) {
            response.put("dailySales", 0.0);
        }

        // NOTE: Terminal counts REMOVED â€” terminal concept eliminated

        try {
            response.put("registeredCustomers", (int) customerRepository.count());
        } catch (Exception e) {
            response.put("registeredCustomers", 0);
        }

        return response;
    }

    @Override
    public List<UserDTO> getUsersByBranchId(Long id) {
        // NOTE: Since users aren't tied to branches anymore,
        // return all active users (this endpoint may be deprecated)
        return userRepository.findAll().stream()
                .filter(u -> u.getAccountStatus() != null &&
                        u.getAccountStatus().name().equals("ACTIVE"))
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToUserDTO(UserProfile user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null);
        return dto;
    }

    // Mapping helpers
    private BranchDTO toDTO(Branch b) {
        if (b == null) return null;
        BranchDTO dto = new BranchDTO();
        dto.setBranchId(b.getBranchId());
        dto.setName(b.getName());
        dto.setCode(b.getCode());
        dto.setAddress(b.getAddress());
        dto.setLocation(b.getLocation());
        dto.setPhone(b.getPhone());
        dto.setEmail(b.getEmail());
        dto.setIsActive(b.getIsActive());
        dto.setCreatedAt(b.getCreatedAt());

        // NOTE: Manager assignment via branch is removed â€” managers have full visibility
        // NOTE: Terminal counts removed â€” terminal concept eliminated

        try {
            BigDecimal sales = saleRepository.getDailySales(b.getBranchId(), LocalDate.now());
            dto.setDailySales(sales != null ? sales.doubleValue() : 0.0);
        } catch (Exception e) {
            dto.setDailySales(0.0);
        }

        try {
            dto.setRegisteredCustomers((int) customerRepository.count());
        } catch (Exception e) {
            dto.setRegisteredCustomers(0);
        }

        return dto;
    }

    private Branch toEntity(BranchDTO dto) {
        if (dto == null) return null;
        Branch b = new Branch();
        b.setName(dto.getName());
        b.setCode(dto.getCode());
        b.setAddress(dto.getAddress());
        b.setLocation(dto.getLocation());
        b.setPhone(dto.getPhone());
        b.setEmail(dto.getEmail());
        b.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return b;
    }

    private synchronized String generateNextBranchCode() {
        List<Branch> branches = branchRepository.findAll();
        int maxCodeNumber = 0;
        for (Branch branch : branches) {
            String rawCode = branch.getCode() != null ? branch.getCode() : "";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(rawCode);
            if (matcher.find()) {
                try {
                    int num = Integer.parseInt(matcher.group(1));
                    if (num > maxCodeNumber) {
                        maxCodeNumber = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("BR%03d", maxCodeNumber + 1);
    }
}

