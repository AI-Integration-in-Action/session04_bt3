package com.logistics.incident.exception;

public class EtlValidationException extends RuntimeException {
    public EtlValidationException(String message) {
        super(message);
    }

    public EtlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
