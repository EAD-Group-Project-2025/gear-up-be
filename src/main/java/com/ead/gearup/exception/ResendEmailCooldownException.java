package com.ead.gearup.exception;

public class ResendEmailCooldownException extends RuntimeException {
    public ResendEmailCooldownException(String message) {
        super(message);
    }
}
