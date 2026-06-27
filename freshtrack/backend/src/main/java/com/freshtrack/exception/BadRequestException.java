package com.freshtrack.exception;

/** Thrown for invalid client input or business rule violations (400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
