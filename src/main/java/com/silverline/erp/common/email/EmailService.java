package com.silverline.erp.common.email;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}

