package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class SupplierBranchId implements Serializable {

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "branch_id")
    private Long branchId;
}

