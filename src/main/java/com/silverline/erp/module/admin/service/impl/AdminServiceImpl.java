package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.service.AdminService;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    @Override
    public BigDecimal getTodaysSales() {
        BigDecimal todaySales = saleRepository.sumNetTotalForToday();
        return todaySales != null ? todaySales : BigDecimal.ZERO;
    }

    @Override
    public Map<String, Long> getUserStatsByRole() {
        Map<String, Long> stats = new LinkedHashMap<>();
        List<UserProfile> allUsers = userRepository.findAll();

        // Group users by role and count
        Map<Role, Long> roleCounts = allUsers.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(UserProfile::getRole, Collectors.counting()));

        // Convert to string keys for JSON response
        for (Role role : Role.values()) {
            stats.put(role.name(), roleCounts.getOrDefault(role, 0L));
        }

        stats.put("TOTAL", (long) allUsers.size());
        return stats;
    }

    @Override
    public List<Map<String, Object>> getTopBranches(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Object[]> topBranches = saleRepository.findTopBranches(limit);
        for (Object[] row : topBranches) {
            Map<String, Object> branchData = new LinkedHashMap<>();
            branchData.put("branchId", row[0]);
            branchData.put("branchName", row[1]);
            branchData.put("totalSales", row[2]);
            result.add(branchData);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getWeeklySalesTrend() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Object[]> dailySales = saleRepository.findLastNDaysSales(7);
        for (Object[] row : dailySales) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", row[0]);
            dayData.put("totalSales", row[1]);
            result.add(dayData);
        }

        return result;
    }

    @Override
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // Sales metrics
        overview.put("todaySales", getTodaysSales());
        BigDecimal allTimeSales = saleRepository.sumNetTotalAllTime();
        overview.put("totalSales", allTimeSales != null ? allTimeSales : BigDecimal.ZERO);

        // User metrics
        overview.put("userStats", getUserStatsByRole());
        overview.put("totalUsers", userRepository.count());

        // Branch metrics
        overview.put("totalBranches", branchRepository.count());
        overview.put("activeBranches", branchRepository.findByIsActiveTrue().size());

        // Top performers
        overview.put("topBranches", getTopBranches(5));
        overview.put("weeklySalesTrend", getWeeklySalesTrend());

        return overview;
    }
}

