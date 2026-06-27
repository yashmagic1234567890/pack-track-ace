package com.freshtrack.exception;

/** Thrown when a requested resource cannot be found (404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
