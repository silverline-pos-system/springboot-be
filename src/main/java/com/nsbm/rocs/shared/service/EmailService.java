package com.nsbm.rocs.shared.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}

