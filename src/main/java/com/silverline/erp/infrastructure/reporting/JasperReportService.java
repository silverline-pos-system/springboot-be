package com.silverline.erp.infrastructure.reporting;

import com.silverline.erp.domain.procurement.Grn;
import com.silverline.erp.domain.procurement.Supplier;
import com.silverline.erp.module.analytics.dto.SalesReportDTO;
import com.silverline.erp.module.analytics.service.SalesAnalyticsService;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.manager.dto.ActivityLogDTO;
import com.silverline.erp.module.manager.dto.ApprovalDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.service.ManagerService;
import com.silverline.erp.module.pos.service.CustomerService;
import com.silverline.erp.module.procurement.repository.GrnRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JasperReportService {

    @Autowired
    private ManagerService managerService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SalesAnalyticsService salesAnalyticsService;

    @Autowired
    private GrnRepository grnRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public byte[] generateApprovalHistoryPdf(Long branchId) throws Exception {
        List<ApprovalDTO> approvals = managerService.getApprovals(null, branchId);
        return generatePdf("/reports/approval_history.jrxml", approvals, null);
    }

    public byte[] generateSalesReportsPdf(String startDate, String endDate, Long branchId) throws Exception {
        List<SalesReportDTO> reports = salesAnalyticsService.getSalesReports(startDate, endDate, branchId);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", startDate);
        parameters.put("endDate", endDate);
        return generatePdf("/reports/sales_report.jrxml", reports, parameters);
    }

    public byte[] generateBranchActivityLogPdf(int limit, Long branchId) throws Exception {
        List<ActivityLogDTO> activities = managerService.getBranchActivityLog(limit, branchId);
        return generatePdf("/reports/branch_activity_log.jrxml", activities, null);
    }

    public byte[] generateLoyaltyCustomersPdf() throws Exception {
        List<ManagerCustomerDTO> customers = customerService.getAllCustomers(org.springframework.data.domain.Pageable.unpaged()).getContent();
        return generatePdf("/reports/loyalty_customers.jrxml", customers, null);
    }

    public byte[] generateDispatchListPdf() throws Exception {
        List<Grn> grns = grnRepository.findAll();
        List<Long> supplierIds = grns.stream()
                .map(d -> d.getSupplierId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> supplierNames = supplierIds.isEmpty() ? Map.of() :
                supplierRepository.findAllById(supplierIds).stream()
                        .collect(Collectors.toMap(s -> s.getSupplierId(), s -> s.getName()));

        List<DispatchReportDTO> reportData = grns.stream()
                .map(grn -> DispatchReportDTO.builder()
                        .dispatchNo(grn.getGrnNo())
                        .supplierName(supplierNames.getOrDefault(grn.getSupplierId(), "Unknown"))
                        .dispatchDate(grn.getGrnDate() != null ? grn.getGrnDate().toString() : "")
                        .totalAmount(grn.getTotalAmount() != null ? grn.getTotalAmount() : BigDecimal.ZERO)
                        .status(grn.getStatus())
                        .build())
                .collect(Collectors.toList());

        return generatePdf("/reports/inventory_dispatch_list.jrxml", reportData, null);
    }

    private byte[] generatePdf(String templatePath, List<?> data, Map<String, Object> parameters) throws Exception {
        InputStream reportStream = getClass().getResourceAsStream(templatePath);
        if (reportStream == null) {
            throw new RuntimeException("Report design file not found: " + templatePath);
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put("createdBy", "ROCS System");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchReportDTO {
        private String dispatchNo;
        private String supplierName;
        private String dispatchDate;
        private BigDecimal totalAmount;
        private String status;
    }
}
