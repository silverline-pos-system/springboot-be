package com.silverline.erp.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String message) {
        super(message, "INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST);
    }
}
