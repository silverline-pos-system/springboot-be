package com.silverline.erp.infrastructure.email;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}

