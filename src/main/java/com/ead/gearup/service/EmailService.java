package com.ead.gearup.service;

public interface EmailService {

    public void sendVerificationEmail(String to, String subject, String text);
}
