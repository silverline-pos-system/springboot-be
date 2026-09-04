package com.silverline.erp.common.exception;

import org.springframework.http.HttpStatus;

public class GrnException extends BusinessException {
    public GrnException(String message) {
        super(message, "GRN_ERROR", HttpStatus.BAD_REQUEST);
    }

    public GrnException(String message, Throwable cause) {
        super(message, "GRN_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
