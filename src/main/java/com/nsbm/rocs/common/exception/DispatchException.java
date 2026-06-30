package com.nsbm.rocs.common.exception;

import org.springframework.http.HttpStatus;

public class DispatchException extends BusinessException {
    public DispatchException(String message) {
        super(message, "DISPATCH_ERROR", HttpStatus.BAD_REQUEST);
    }

    public DispatchException(String message, Throwable cause) {
        super(message, "DISPATCH_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
