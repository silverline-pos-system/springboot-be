package com.nsbm.rocs.modules.inventory.exception;

public class DispatchException extends RuntimeException {
    
    public DispatchException(String message) {
        super(message);
    }

    public DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
