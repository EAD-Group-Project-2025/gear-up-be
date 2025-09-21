package com.ead.gearup.exception;

public class UnauthorizedCustomerAccessException extends RuntimeException {
    public UnauthorizedCustomerAccessException(String message) {
        super(message);
    }
}