package com.silverline.erp.module.auth.service;

public interface PasswordResetRequestService {
    void forgotPassword(String username, String newPassword, String reason);
}
