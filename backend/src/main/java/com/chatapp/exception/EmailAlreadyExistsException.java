package com.chatapp.exception;

/**
 * ============================================================================
 * EMAIL ALREADY EXISTS EXCEPTION
 * ============================================================================
 * 
 * This exception is thrown when a user tries to register with an email address
 * that is already registered in the system.
 * 
 * WHEN IS THIS THROWN?
 * --------------------
 * During registration in AuthService.register():
 * 
 *   if (userRepository.existsByEmail(request.getEmail())) {
 *       throw new EmailAlreadyExistsException("Email already registered");
 *   }
 * 
 * WHAT HAPPENS WHEN THROWN?
 * -------------------------
 * 1. Exception bubbles up from AuthService → AuthController
 * 2. GlobalExceptionHandler catches it
 * 3. Returns HTTP 409 Conflict to the client:
 *    {
 *      "timestamp": "2026-01-04T10:30:00",
 *      "status": 409,
 *      "error": "Conflict",
 *      "message": "Email already registered"
 *    }
 * 
 * WHY 409 CONFLICT?
 * -----------------
 * HTTP 409 means "the request conflicts with the current state of the server."
 * In this case, the email already exists, so creating a new user with it
 * would conflict with the existing user.
 * 
 * WHY EXTEND RuntimeException?
 * ----------------------------
 * RuntimeException is an "unchecked" exception, meaning:
 * - No need to declare it in method signatures (throws clause)
 * - No need to wrap in try-catch everywhere
 * - Cleaner code, handled globally by GlobalExceptionHandler
 * 
 * Checked vs Unchecked Exceptions:
 * - Checked (extends Exception): Must be caught or declared
 * - Unchecked (extends RuntimeException): Optional to catch
 * 
 * @see GlobalExceptionHandler#handleEmailAlreadyExists Where this is caught
 * @see AuthService#register Where this is thrown
 */
public class EmailAlreadyExistsException extends RuntimeException {
    
    /**
     * Creates a new EmailAlreadyExistsException with a message.
     * 
     * The message will be included in the error response sent to the client.
     * 
     * @param message Description of the error (e.g., "Email already registered")
     */
    public EmailAlreadyExistsException(String message) {
        // super() calls the parent class (RuntimeException) constructor
        // This stores the message so getMessage() can retrieve it later
        super(message);
    }
}