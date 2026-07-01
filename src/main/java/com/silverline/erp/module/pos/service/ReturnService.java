package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.pos.dto.returns.ReturnRequest;

public interface ReturnService {
    Long processReturn(ReturnRequest request);
}
