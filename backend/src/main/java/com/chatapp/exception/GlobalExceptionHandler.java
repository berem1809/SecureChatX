package com.chatapp.exception;

// ============================================================================
// IMPORTS
// ============================================================================

// SLF4J Logging - Standard logging facade for Java
import org.slf4j.Logger;           // Interface for logging
import org.slf4j.LoggerFactory;    // Factory to create loggers

// Spring Framework imports
import org.springframework.http.HttpStatus;        // Enum of HTTP status codes (200, 401, 404, etc.)
import org.springframework.http.ResponseEntity;    // Wrapper for HTTP responses

// Spring Security exceptions
import org.springframework.security.access.AccessDeniedException;        // 403 Forbidden
import org.springframework.security.authentication.BadCredentialsException; // Wrong password

// Spring Web annotation
import org.springframework.web.bind.annotation.ExceptionHandler;    // Marks exception handler methods
import org.springframework.web.bind.annotation.RestControllerAdvice; // Global exception handler

// Java utilities
import java.time.LocalDateTime;   // For timestamps in error responses
import java.util.HashMap;         // For building error response map
import java.util.Map;             // Interface for key-value pairs

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER - Centralized error handling for the entire API
 * ============================================================================
 * 
 * This class catches exceptions thrown anywhere in the application and converts
 * them into proper HTTP error responses with appropriate status codes.
 * 
 * WHY GLOBAL EXCEPTION HANDLING?
 * ------------------------------
 * Without this class, exceptions would result in ugly stack traces or
 * generic 500 errors. With it, we return clean, consistent JSON responses.
 * 
 * BEFORE (without handler):
 * -------------------------
 * {
 *   "timestamp": "2026-01-04T10:30:00.000+00:00",
 *   "status": 500,
 *   "error": "Internal Server Error",
 *   "trace": "com.chatapp.exception.EmailAlreadyExistsException: Email already...\n\tat com.chatapp.service...",
 *   "path": "/api/auth/register"
 * }
 * 
 * AFTER (with handler):
 * ---------------------
 * {
 *   "timestamp": "2026-01-04T10:30:00",
 *   "status": 409,
 *   "error": "Conflict",
 *   "message": "Email already registered"
 * }
 * 
 * HOW IT WORKS:
 * -------------
 * 1. Exception is thrown somewhere in the application
 * 2. Spring looks for @ExceptionHandler that matches the exception type
 * 3. The matching handler method is called
 * 4. Handler returns a ResponseEntity with error details
 * 5. Client receives clean JSON error response
 * 
 * EXCEPTION HANDLER PRIORITY:
 * ---------------------------
 * Handlers are matched from MOST SPECIFIC to LEAST SPECIFIC:
 * 1. EmailAlreadyExistsException (exact match)
 * 2. RuntimeException (parent class)
 * 3. Exception (catches everything)
 * 
 * HTTP STATUS CODES WE USE:
 * -------------------------
 * | Code | Name                | When Used                              |
 * |------|---------------------|----------------------------------------|
 * | 400  | Bad Request         | Invalid input, general errors          |
 * | 401  | Unauthorized        | Invalid credentials, expired tokens    |
 * | 403  | Forbidden           | Authenticated but no permission        |
 * | 409  | Conflict            | Email already exists                   |
 * | 429  | Too Many Requests   | Rate limit exceeded                    |
 * | 500  | Internal Server Err | Unexpected errors (bugs)               |
 * 
 * @RestControllerAdvice ANNOTATION:
 * ---------------------------------
 * This is a combination of:
 * - @ControllerAdvice: Applies to all controllers
 * - @ResponseBody: Returns data directly (not view names)
 * 
 * Together they mean: \"Handle exceptions from all REST controllers and
 * return the result as JSON response body.\"
 */
@RestControllerAdvice  // Makes this a global exception handler for all @RestController classes
public class GlobalExceptionHandler {

    /**
     * Logger for recording exception information.
     * 
     * WHY LOG EXCEPTIONS?
     * - Debugging: See what went wrong in production
     * - Security: Track failed login attempts
     * - Monitoring: Alert on unusual error patterns
     * 
     * LOG LEVELS USED:
     * - logger.warn(): Expected errors (wrong password, duplicate email)
     * - logger.error(): Unexpected errors (bugs, database down)
     */
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========================================================================
    // CUSTOM EXCEPTION HANDLERS
    // ========================================================================

    /**
     * Handles EmailAlreadyExistsException - when user registers with existing email.
     * 
     * HTTP 409 CONFLICT: The request conflicts with the current state of the resource.
     * In this case, the email already belongs to another user.
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 409 status and error details
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)  // Catches this specific exception type
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        // Log as WARNING because this is expected behavior (user error, not system error)
        logger.warn("Email already exists: {}", ex.getMessage());
        
        // Return 409 Conflict with the error message
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles InvalidTokenException - when verification or refresh token is invalid.
     * 
     * HTTP 401 UNAUTHORIZED: Authentication is required and has failed.
     * The token is missing, expired, or tampered with.
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 401 status and error details
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        // Log as WARNING - this could be normal (expired token) or suspicious (forged token)
        logger.warn("Invalid token: {}", ex.getMessage());
        
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ========================================================================
    // SPRING SECURITY EXCEPTION HANDLERS
    // ========================================================================

    /**
     * Handles AccessDeniedException - when authenticated user lacks permission.
     * 
     * HTTP 403 FORBIDDEN: The server understood the request but refuses to authorize it.
     * Unlike 401, the user IS authenticated, but doesn't have permission.
     * 
     * DIFFERENCE BETWEEN 401 AND 403:
     * - 401 Unauthorized: \"Who are you? Please log in.\"
     * - 403 Forbidden: \"I know who you are, but you can't do this.\"
     * 
     * EXAMPLE:
     * A regular user trying to access admin endpoints would get 403.
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 403 status and error details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        
        // Use generic message - don't reveal what resource they tried to access
        return buildErrorResponse(
            HttpStatus.FORBIDDEN, 
            "Access denied. You don't have permission to access this resource."
        );
    }

    /**
     * Handles BadCredentialsException - when login password is wrong.
     * 
     * HTTP 401 UNAUTHORIZED: The credentials are invalid.
     * 
     * SECURITY NOTE:
     * We use a generic message \"Invalid email or password\" instead of
     * \"Wrong password\" to prevent attackers from knowing if the email exists.
     * This is called \"user enumeration protection.\"
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 401 status and error details
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        // Don't log the password! Just note that an attempt failed
        logger.warn("Bad credentials attempt");
        
        // Generic message to prevent user enumeration attacks
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // ========================================================================
    // RATE LIMITING EXCEPTION HANDLER
    // ========================================================================

    /**
     * Handles RateLimitExceededException - when user makes too many requests.
     * 
     * HTTP 429 TOO MANY REQUESTS: The user has sent too many requests in a given time.
     * 
     * This helps protect against:
     * - Brute-force password attacks
     * - Denial of Service (DoS) attempts
     * - API abuse
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 429 status and error details
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        // Log with client info for security monitoring
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // ========================================================================
    // CHAT & CONVERSATION EXCEPTION HANDLERS
    // ========================================================================

    /**
     * Handles ChatRequestAlreadyExistsException - when duplicate chat request is sent.
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(ChatRequestAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleChatRequestAlreadyExists(ChatRequestAlreadyExistsException ex) {
        logger.warn("Chat request already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles ConversationAlreadyExistsException - when duplicate conversation is created.
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(ConversationAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConversationAlreadyExists(ConversationAlreadyExistsException ex) {
        logger.warn("Conversation already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles ConversationAccessDeniedException - when user lacks conversation access.
     * HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleConversationAccessDenied(ConversationAccessDeniedException ex) {
        logger.warn("Conversation access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles ChatRequestNotFoundException - when chat request is not found.
     * HTTP 404 NOT FOUND
     */
    @ExceptionHandler(ChatRequestNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleChatRequestNotFound(ChatRequestNotFoundException ex) {
        logger.warn("Chat request not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles ConversationNotFoundException - when conversation is not found.
     * HTTP 404 NOT FOUND
     */
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConversationNotFound(ConversationNotFoundException ex) {
        logger.warn("Conversation not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ========================================================================
    // GROUP EXCEPTION HANDLERS
    // ========================================================================

    /**
     * Handles GroupAccessDeniedException - when user lacks group access.
     * HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(GroupAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleGroupAccessDenied(GroupAccessDeniedException ex) {
        logger.warn("Group access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles GroupNotFoundException - when group is not found.
     * HTTP 404 NOT FOUND
     */
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGroupNotFound(GroupNotFoundException ex) {
        logger.warn("Group not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles GroupInvitationNotFoundException - when group invitation is not found.
     * HTTP 404 NOT FOUND
     */
    @ExceptionHandler(GroupInvitationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGroupInvitationNotFound(GroupInvitationNotFoundException ex) {
        logger.warn("Group invitation not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles GroupInvitationAlreadyExistsException - when duplicate invitation is sent.
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(GroupInvitationAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleGroupInvitationAlreadyExists(GroupInvitationAlreadyExistsException ex) {
        logger.warn("Group invitation already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles UserAlreadyGroupMemberException - when user is already a group member.
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(UserAlreadyGroupMemberException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyGroupMember(UserAlreadyGroupMemberException ex) {
        logger.warn("User already group member: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles UserNotFoundException - when user is not found.
     * HTTP 404 NOT FOUND
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ========================================================================
    // GENERIC EXCEPTION HANDLERS (Catch-all)
    // ========================================================================

    /**
     * Handles RuntimeException - catches most application exceptions.
     * 
     * HTTP 400 BAD REQUEST: Generic client error.
     * Used for validation errors, business logic violations, etc.
     * 
     * NOTE: This is a catch-all for RuntimeExceptions not handled above.
     * Consider adding specific handlers for common exceptions.
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 400 status and error details
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // Log as ERROR with stack trace because this might be a bug
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles Exception - the ultimate catch-all for any unhandled exception.
     * 
     * HTTP 500 INTERNAL SERVER ERROR: Something went wrong on the server.
     * 
     * This should rarely be triggered if we have proper exception handling.
     * If you see 500 errors, it usually means a bug in the code.
     * 
     * SECURITY NOTE:
     * We return a generic message instead of exception details to avoid
     * leaking sensitive information (database names, file paths, etc.)
     * 
     * @param ex The exception that was thrown
     * @return ResponseEntity with 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log FULL details including stack trace for debugging
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        // Return GENERIC message to client (don't expose internal details!)
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred"
        );
    }

    // ========================================================================
    // HELPER METHOD - Builds consistent error response format
    // ========================================================================

    /**
     * Creates a standardized error response object.
     * 
     * All our error responses have the same structure for consistency:
     * {
     *   \"timestamp\": \"2026-01-04T10:30:00\",
     *   \"status\": 401,
     *   \"error\": \"Unauthorized\",
     *   \"message\": \"Invalid credentials\"
     * }
     * 
     * WHY STANDARDIZE?
     * - Frontend can parse all errors the same way
     * - Easy to debug and understand
     * - Professional API design
     * 
     * @param status The HTTP status (e.g., HttpStatus.UNAUTHORIZED)
     * @param message The error message to display to the user
     * @return ResponseEntity with the error details
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        // Create a map to hold error details
        Map<String, Object> error = new HashMap<>();
        
        // Add timestamp for debugging (when did this error occur?)
        error.put("timestamp", LocalDateTime.now().toString());
        
        // Add numeric status code (e.g., 401, 403, 409)
        error.put("status", status.value());
        
        // Add status reason phrase (e.g., \"Unauthorized\", \"Forbidden\")
        error.put("error", status.getReasonPhrase());
        
        // Add our custom error message
        error.put("message", message);
        
        // Create ResponseEntity with status code and body
        return ResponseEntity.status(status).body(error);
    }
}