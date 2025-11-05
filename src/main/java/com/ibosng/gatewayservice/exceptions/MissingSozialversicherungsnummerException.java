package com.ibosng.gatewayservice.exceptions;

/**
 * Exception thrown when a Teilnehmer's Sozialversicherungsnummer is missing or invalid.
 * This is a runtime exception that should be caught by the global exception handler.
 */
public class MissingSozialversicherungsnummerException extends RuntimeException {

    public MissingSozialversicherungsnummerException(String message) {
        super(message);
    }

    public MissingSozialversicherungsnummerException(String message, Throwable cause) {
        super(message, cause);
    }
}