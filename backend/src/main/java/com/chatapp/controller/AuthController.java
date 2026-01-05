package com.chatapp.controller;

// ============================================================================
// IMPORTS - External libraries and internal classes needed by this controller
// ============================================================================

// DTO (Data Transfer Object) classes - These carry data between client and server
import com.chatapp.dto.AuthResponse;      // Response object containing JWT tokens
import com.chatapp.dto.LoginRequest;       // Request object for login (email + password)
import com.chatapp.dto.RegisterRequest;    // Request object for registration (email + password + displayName)

// Repository for database access (not directly used here, but available if needed)
import com.chatapp.repository.UserRepository;

// Service layer - contains business logic for authentication
import com.chatapp.service.AuthService;

// Spring Framework imports
import org.springframework.http.MediaType;        // Defines response content types (JSON, HTML, etc.)
import org.springframework.http.ResponseEntity;   // Wrapper for HTTP responses with status codes

// Jakarta Servlet imports (for handling HTTP requests/responses and cookies)
import jakarta.servlet.http.Cookie;               // Represents an HTTP cookie
import jakarta.servlet.http.HttpServletRequest;   // Represents the incoming HTTP request
import jakarta.servlet.http.HttpServletResponse;  // Represents the outgoing HTTP response

// Spring Web annotations for building REST APIs
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * AUTH CONTROLLER - Handles all authentication-related HTTP endpoints
 * ============================================================================
 * 
 * This controller is the entry point for all authentication operations.
 * It receives HTTP requests from clients (like Postman, web browsers, or mobile apps)
 * and delegates the actual work to AuthService.
 * 
 * AVAILABLE ENDPOINTS:
 * --------------------
 * 1. POST /api/auth/register  - Create a new user account
 *    - Input: email, password, displayName (JSON body)
 *    - Output: 200 OK (success) or error response
 *    - Action: Creates user in database + sends verification email
 * 
 * 2. GET /api/auth/verify     - Verify user's email address
 *    - Input: token (URL parameter) - e.g., /api/auth/verify?token=abc123
 *    - Output: HTML page showing success or failure
 *    - Action: Activates user account if token is valid
 * 
 * 3. POST /api/auth/login     - Authenticate and get JWT tokens
 *    - Input: email, password (JSON body)
 *    - Output: accessToken in response body, refreshToken in HttpOnly cookie
 *    - Action: Validates credentials and issues JWT tokens
 * 
 * 4. POST /api/auth/refresh   - Get a new access token using refresh token
 *    - Input: refreshToken (from cookie or body)
 *    - Output: New accessToken
 *    - Action: Issues new access token without re-login
 * 
 * 5. POST /api/auth/logout    - End user session
 *    - Input: refreshToken (from cookie or body)
 *    - Output: 200 OK
 *    - Action: Invalidates refresh token and clears cookie
 * 
 * SECURITY NOTES:
 * ---------------
 * - Access tokens are short-lived (15 minutes) and sent in response body
 * - Refresh tokens are long-lived (30 days) and stored in HttpOnly cookies
 * - HttpOnly cookies cannot be accessed by JavaScript (prevents XSS attacks)
 * - Secure=true means cookies only sent over HTTPS (prevents interception)
 * 
 * @author ChatApp Team
 * @version 1.0
 */
@RestController  // Tells Spring this class handles REST API requests and returns JSON/data (not views)
@RequestMapping("/api/auth")  // Base URL path - all endpoints in this controller start with /api/auth
public class AuthController {
    
    // ========================================================================
    // DEPENDENCY INJECTION
    // ========================================================================
    
    /**
     * AuthService handles all the business logic for authentication.
     * We use "dependency injection" - Spring automatically provides this service.
     * This is marked as 'final' because it should never change after construction.
     */
    private final AuthService authService;

    /**
     * CONSTRUCTOR - Called by Spring when creating this controller
     * 
     * Spring automatically finds the AuthService bean and passes it here.
     * This is called "Constructor Injection" - the recommended way to inject dependencies.
     * 
     * WHY CONSTRUCTOR INJECTION?
     * - Makes dependencies explicit and required
     * - Allows the field to be 'final' (immutable)
     * - Makes testing easier (can pass mock services)
     * 
     * @param authService The authentication service (injected by Spring)
     */
    public AuthController(AuthService authService) { 
        this.authService = authService; 
    }

    // ========================================================================
    // ENDPOINT: USER REGISTRATION
    // ========================================================================
    
    /**
     * POST /api/auth/register - Register a new user account
     * 
     * FLOW:
     * 1. Client sends POST request with JSON body containing email, password, displayName
     * 2. This method receives the data as a RegisterRequest object
     * 3. Calls authService.register() which:
     *    a. Checks if email already exists
     *    b. Hashes the password with BCrypt
     *    c. Saves user to MySQL database with status "PENDING_VERIFICATION"
     *    d. Creates verification token in Redis (expires in 24 hours)
     *    e. Sends verification email with link
     * 4. Returns 200 OK if successful
     * 
     * EXAMPLE REQUEST:
     * POST http://localhost:8080/api/auth/register
     * Content-Type: application/json
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePass123!",
     *   "displayName": "John Doe"
     * }
     * 
     * POSSIBLE RESPONSES:
     * - 200 OK: Registration successful, check email for verification link
     * - 409 Conflict: Email already registered
     * - 400 Bad Request: Invalid input data
     * 
     * @param request The registration data from the client (automatically parsed from JSON)
     * @return ResponseEntity with appropriate HTTP status
     */
    @PostMapping("/register")  // Maps POST requests to /api/auth/register
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // @RequestBody tells Spring to parse the JSON body into a RegisterRequest object
        // The <?> means this method can return any type of response body
        
        authService.register(request);  // Delegate to service layer
        
        // Return HTTP 200 OK with empty body (success indicator)
        // If anything goes wrong, an exception is thrown and handled by GlobalExceptionHandler
        return ResponseEntity.ok().build();
    }

    // ========================================================================
    // ENDPOINT: EMAIL VERIFICATION
    // ========================================================================
    
    /**
     * GET /api/auth/verify - Verify user's email address
     * 
     * This endpoint is accessed when user clicks the link in their verification email.
     * The link format is: http://localhost:8080/api/auth/verify?token=uuid-here
     * 
     * FLOW:
     * 1. User clicks verification link in email
     * 2. Browser sends GET request with token as URL parameter
     * 3. authService.verifyEmail() validates the token:
     *    a. Looks up token in Redis to find userId
     *    b. Updates user status to "ACTIVE" in MySQL
     *    c. Deletes the verification token from Redis
     * 4. Returns HTML page showing success or failure
     * 
     * WHY RETURN HTML INSTEAD OF JSON?
     * - This endpoint is accessed directly by browser (clicking email link)
     * - Users expect to see a readable page, not raw JSON
     * - The page includes a link to the login page
     * 
     * @param token The verification token from the URL (extracted by @RequestParam)
     * @return HTML page indicating success or failure
     */
    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)  // Returns HTML, not JSON
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        // @RequestParam extracts the "token" value from URL query string
        // Example: /api/auth/verify?token=abc123 -> token = "abc123"
        
        try {
            // Attempt to verify the email
            authService.verifyEmail(token);
            
            // SUCCESS: Build an HTML page to show the user
            // Using Java text blocks (triple quotes) for multi-line strings
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Email Verified</title>
                    <meta http-equiv="refresh" content="3;url=http://localhost:5173/login" />
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .success { color: green; }
                    </style>
                </head>
                <body>
                    <h1 class="success">Email Verified Successfully!</h1>
                    <p>Your email has been verified. You can now log in to your account.</p>
                    <p>Redirecting to login page in 3 seconds...</p>
                    <p><a href="http://localhost:5173/login">Click here if not redirected</a></p>
                </body>
                </html>
                """;
            return ResponseEntity.ok(html);  // HTTP 200 with HTML body
            
        } catch (Exception e) {
            // FAILURE: Token is invalid or expired
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Verification Failed</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .error { color: red; }
                    </style>
                </head>
                <body>
                    <h1 class="error">Verification Failed</h1>
                    <p>Invalid or expired token. Please try registering again.</p>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().body(html);  // HTTP 400 with HTML body
        }
    }

    // ========================================================================
    // ENDPOINT: USER LOGIN
    // ========================================================================
    
    /**
     * POST /api/auth/login - Authenticate user and issue JWT tokens
     * 
     * This is the main authentication endpoint. Users provide credentials
     * and receive JWT tokens for subsequent API calls.
     * 
     * FLOW:
     * 1. Client sends POST with email and password
     * 2. authService.login() validates credentials:
     *    a. Finds user in MySQL by email
     *    b. Verifies password using BCrypt
     *    c. Checks that user status is "ACTIVE" (email verified)
     *    d. Generates JWT access token (15 min expiry)
     *    e. Generates JWT refresh token (30 day expiry)
     *    f. Stores refresh token hash in Redis
     * 3. Sets refresh token in HttpOnly cookie (security best practice)
     * 4. Returns access token in response body
     * 
     * TWO-TOKEN STRATEGY:
     * - Access Token: Short-lived (15 min), sent in Authorization header for API calls
     * - Refresh Token: Long-lived (30 days), stored in HttpOnly cookie
     * 
     * WHY SEPARATE TOKENS?
     * - If access token is stolen, it expires quickly (limited damage)
     * - Refresh token is more secure in HttpOnly cookie (can't be stolen by XSS)
     * - User stays logged in for 30 days without re-entering password
     * 
     * EXAMPLE REQUEST:
     * POST http://localhost:8080/api/auth/login
     * Content-Type: application/json
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePass123!"
     * }
     * 
     * EXAMPLE RESPONSE:
     * {
     *   "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
     * }
     * + Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9... (HttpOnly, Secure)
     * 
     * @param request Login credentials (email + password)
     * @param response HttpServletResponse to add the cookie
     * @return AuthResponse containing the access token
     */
    @PostMapping("/login")  // Maps POST requests to /api/auth/login
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        // Call service to validate credentials and generate tokens
        AuthResponse resp = authService.login(request);
        
        // ====================================================================
        // SET REFRESH TOKEN AS HTTP-ONLY COOKIE
        // ====================================================================
        // This is a security best practice for storing refresh tokens
        
        Cookie cookie = new Cookie("refreshToken", resp.getRefreshToken());
        
        // HttpOnly = true: JavaScript cannot access this cookie (prevents XSS attacks)
        // This is CRITICAL for security - refresh tokens should never be accessible to JS
        cookie.setHttpOnly(true);
        
        // Secure = true: Cookie only sent over HTTPS connections
        // In production, this prevents the cookie from being intercepted
        // Note: For local development (http://localhost), browser may still work
        cookie.setSecure(true);
        
        // Path = "/": Cookie is sent with ALL requests to this domain
        // This allows the /api/auth/refresh endpoint to receive the cookie
        cookie.setPath("/");
        
        // MaxAge: How long the cookie lives in the browser (in seconds)
        // 30 days = 30 * 24 * 60 * 60 = 2,592,000 seconds
        cookie.setMaxAge(30 * 24 * 60 * 60);
        
        // Add the cookie to the response headers
        response.addCookie(cookie);
        
        // ====================================================================
        // REMOVE REFRESH TOKEN FROM RESPONSE BODY
        // ====================================================================
        // We don't want to send the refresh token in the JSON response
        // because it's already in the cookie. This encourages secure storage.
        resp.setRefreshToken(null);
        
        // Return access token in response body
        return ResponseEntity.ok(resp);
    }

    // ========================================================================
    // ENDPOINT: REFRESH ACCESS TOKEN
    // ========================================================================
    
    /**
     * POST /api/auth/refresh - Get a new access token using refresh token
     * 
     * When the access token expires (after 15 minutes), the client should call
     * this endpoint to get a new one without requiring the user to log in again.
     * 
     * FLOW:
     * 1. Client sends POST request (refresh token is automatically sent via cookie)
     * 2. We extract refresh token from cookie (or body as fallback)
     * 3. authService.refresh() validates the token:
     *    a. Verifies JWT signature
     *    b. Checks token exists in Redis
     *    c. Gets user from MySQL
     *    d. Generates new access token
     * 4. Returns new access token
     * 
     * TOKEN SOURCES (in priority order):
     * 1. HttpOnly Cookie named "refreshToken" (preferred, most secure)
     * 2. Request body as plain string (fallback for testing)
     * 
     * WHEN TO CALL THIS:
     * - When API returns 401 Unauthorized
     * - Before access token expires (proactive refresh)
     * - Typically called automatically by frontend HTTP interceptor
     * 
     * EXAMPLE REQUEST (Cookie method - automatic):
     * POST http://localhost:8080/api/auth/refresh
     * Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9...
     * 
     * EXAMPLE REQUEST (Body method - for testing):
     * POST http://localhost:8080/api/auth/refresh
     * Content-Type: text/plain
     * Body: eyJhbGciOiJIUzUxMiJ9...
     * 
     * @param cookieToken Refresh token from cookie (null if not present)
     * @param refreshToken Refresh token from request body (null if not present)
     * @param response HttpServletResponse to refresh the cookie
     * @return AuthResponse with new access token
     */
    @PostMapping("/refresh")  // Maps POST requests to /api/auth/refresh
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookieToken,  // From cookie
            @RequestBody(required = false) String refreshToken,  // From body (fallback)
            HttpServletResponse response) {
        
        // Use cookie token if available, otherwise use body token
        // This allows both secure cookie-based refresh and manual testing
        String token = cookieToken != null ? cookieToken : refreshToken;
        
        // Call service to validate and generate new access token
        AuthResponse resp = authService.refresh(token);
        
        // Refresh the cookie (update expiry time)
        // This extends the session each time user refreshes their token
        Cookie cookie = new Cookie("refreshToken", resp.getRefreshToken());
        cookie.setHttpOnly(true);   // Can't be accessed by JavaScript
        cookie.setSecure(true);     // Only sent over HTTPS
        cookie.setPath("/");        // Available for all paths
        cookie.setMaxAge(30 * 24 * 60 * 60);  // Reset 30-day expiry
        response.addCookie(cookie);
        
        // Don't include refresh token in response body
        resp.setRefreshToken(null);
        
        return ResponseEntity.ok(resp);
    }

    // ========================================================================
    // ENDPOINT: USER LOGOUT
    // ========================================================================
    
    /**
     * POST /api/auth/logout - End user session and invalidate tokens
     * 
     * This endpoint performs a "secure logout" by:
     * 1. Deleting the refresh token from Redis (server-side invalidation)
     * 2. Clearing the refresh token cookie (client-side cleanup)
     * 
     * IMPORTANT SECURITY NOTE:
     * - Access tokens cannot be invalidated (they're stateless JWTs)
     * - Access tokens will remain valid until they expire (15 min)
     * - For immediate security, rely on short access token expiry
     * - Refresh token invalidation prevents getting new access tokens
     * 
     * FLOW:
     * 1. Extract refresh token from cookie (or body)
     * 2. Delete refresh token from Redis
     * 3. Clear the cookie by setting MaxAge to 0
     * 4. Return 200 OK
     * 
     * AFTER LOGOUT:
     * - User cannot refresh their access token
     * - Current access token works for up to 15 more minutes
     * - User must log in again to get new tokens
     * 
     * @param cookieToken Refresh token from cookie
     * @param refreshToken Refresh token from body (fallback)
     * @param response HttpServletResponse to clear the cookie
     * @return Empty 200 OK response
     */
    @PostMapping("/logout")  // Maps POST requests to /api/auth/logout
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) String refreshToken,
            HttpServletResponse response) {
        
        // Use cookie token if available, otherwise use body token
        String token = cookieToken != null ? cookieToken : refreshToken;
        
        // Delete refresh token from Redis (if token exists)
        // This invalidates the session on the server side
        if (token != null) {
            authService.logout(token);
        }
        
        // ====================================================================
        // CLEAR THE COOKIE
        // ====================================================================
        // Setting MaxAge to 0 tells the browser to delete the cookie immediately
        
        Cookie cookie = new Cookie("refreshToken", "");  // Empty value
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // Delete immediately
        response.addCookie(cookie);
        
        // Return success (empty body)
        return ResponseEntity.ok().build();
    }
}
