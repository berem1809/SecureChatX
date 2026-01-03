package com.chatapp.exception;

/**
 * Exception thrown when a token is invalid or expired.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}