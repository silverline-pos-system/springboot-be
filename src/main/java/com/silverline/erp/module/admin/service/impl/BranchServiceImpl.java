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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;

    // NOTE: TerminalRepository and UserBranchRepository REMOVED

    @Autowired
    public BranchServiceImpl(BranchRepository branchRepository,
                             UserRepository userRepository,
                             SaleRepository saleRepository,
                             CustomerRepository customerRepository) {
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
    }

    // Create
    @Override
    public BranchDTO createBranch(BranchDTO dto) {
        Branch entity = toEntity(dto);
        Branch saved = branchRepository.save(entity);
        return toDTO(saved);
    }

    // Read all
    @Override
    public List<BranchDTO> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Read by id
    @Override
    public BranchDTO getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        return toDTO(branch);
    }

    // Update
    @Override
    public BranchDTO updateBranch(Long id, BranchDTO dto) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

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
    @Override
    public void deleteBranch(Long id) {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branchRepository.delete(existing);
    }

    @Override
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
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        
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
}

