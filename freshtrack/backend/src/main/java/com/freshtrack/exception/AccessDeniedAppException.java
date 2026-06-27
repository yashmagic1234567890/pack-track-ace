package com.freshtrack.exception;

/** Thrown when a Hub User attempts to access a warehouse they are not mapped to (403). */
public class AccessDeniedAppException extends RuntimeException {
    public AccessDeniedAppException(String message) {
        super(message);
    }
}
