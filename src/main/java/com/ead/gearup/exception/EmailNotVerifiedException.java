package com.ead.gearup.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Email not verified");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
