package com.nsbm.rocs.modules.services.repository;

import com.nsbm.rocs.entity.services.RepairJob;
import com.nsbm.rocs.entity.enums.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepairJobRepository extends JpaRepository<RepairJob, Long> {

    Optional<RepairJob> findByRepairNo(String repairNo);

    List<RepairJob> findByBranchId(Long branchId);

    List<RepairJob> findByStatus(RepairStatus status);
    List<RepairJob> findByTechnicianId(Long technicianId);
    
    List<RepairJob> findByCustomerId(Long customerId);
    List<RepairJob> findByCustomerIdIn(List<Long> customerIds);
    List<RepairJob> findByDeviceModelContainingIgnoreCase(String deviceModel);
    List<RepairJob> findByDeviceBrandContainingIgnoreCase(String deviceBrand);
    List<RepairJob> findByRepairNoContainingIgnoreCase(String repairNo);
}

