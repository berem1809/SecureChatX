package com.chatapp.security;

// ============================================================================
// IMPORTS
// ============================================================================

// Model and Repository for accessing user data from database
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;

// Spring Security imports for authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

// Spring utility for string operations
import org.springframework.util.StringUtils;

// Spring base class for filters that should only run once per request
import org.springframework.web.filter.OncePerRequestFilter;

// Jakarta Servlet imports for HTTP request/response handling
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * ============================================================================
 * JWT AUTHENTICATION FILTER
 * ============================================================================
 * 
 * This filter intercepts EVERY incoming HTTP request and checks for a valid JWT
 * access token in the Authorization header. If found and valid, it sets up
 * the Spring Security context so the user is considered "authenticated".
 * 
 * WHAT IS A FILTER?
 * -----------------
 * Filters are components that process requests BEFORE they reach your controllers.
 * Think of them as security guards that check your credentials before letting you in.
 * 
 * Request Flow: Client -> Filter -> Controller -> Service -> Response
 * 
 * HOW JWT AUTHENTICATION WORKS:
 * -----------------------------
 * 1. Client logs in and receives an access token (JWT)
 * 2. For subsequent requests, client includes the token in the Authorization header:
 *    Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
 * 3. This filter extracts and validates the token
 * 4. If valid, the user is authenticated for this request
 * 5. If invalid/missing, the user is NOT authenticated (may get 401/403)
 * 
 * WHAT IS "Bearer"?
 * -----------------
 * "Bearer" is a token type defined in OAuth 2.0 specification.
 * It simply means "whoever BEARS (carries) this token is authorized".
 * The format is: "Bearer <token>" with a space between them.
 * 
 * WHY OncePerRequestFilter?
 * -------------------------
 * - Ensures the filter runs exactly ONCE per request
 * - Prevents issues with request forwarding/dispatching
 * - Standard practice for authentication filters
 * 
 * @see JwtTokenProvider For token generation and validation logic
 * @see SecurityConfig Where this filter is registered
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    
    /**
     * JwtTokenProvider handles all JWT operations:
     * - Generating tokens (in AuthService)
     * - Validating tokens (checking signature and expiry)
     * - Extracting claims (getting user email from token)
     */
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * UserRepository for fetching user details from MySQL database.
     * We need this to get the full user object after validating the token.
     */
    private final UserRepository userRepository;

    /**
     * Constructor - creates the filter with required dependencies.
     * 
     * NOTE: This filter is NOT a Spring @Component, so we manually create it
     * in SecurityConfig and pass the dependencies there.
     * 
     * @param jwtTokenProvider For JWT validation
     * @param userRepository For fetching user data
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    // ========================================================================
    // MAIN FILTER METHOD
    // ========================================================================
    
    /**
     * This method is called for EVERY HTTP request to the application.
     * It checks for a JWT token and authenticates the user if valid.
     * 
     * STEP-BY-STEP PROCESS:
     * 1. Get the "Authorization" header from the request
     * 2. Check if it starts with "Bearer " (note the space)
     * 3. Extract the token (everything after "Bearer ")
     * 4. Validate the token (signature + expiry)
     * 5. If valid, get the user email from the token
     * 6. Load the user from the database
     * 7. Create an Authentication object and set it in SecurityContext
     * 8. Continue with the filter chain (let the request proceed)
     * 
     * @param request The incoming HTTP request
     * @param response The outgoing HTTP response
     * @param filterChain The chain of filters to continue processing
     * @throws ServletException If a servlet error occurs
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws IOException, ServletException {
        
        // ====================================================================
        // STEP 1: Extract JWT Token from Authorization Header
        // ====================================================================
        
        // Get the Authorization header value
        // Example: "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiw..."
        String header = request.getHeader("Authorization");
        
        // Variable to store the extracted token
        String token = null;
        
        // Check if header exists and starts with "Bearer "
        // StringUtils.hasText() checks for null, empty string, and whitespace-only
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // Extract the token part (skip "Bearer " which is 7 characters)
            token = header.substring(7);
        }

        // ====================================================================
        // STEP 2: Validate Token and Set Authentication
        // ====================================================================
        
        // Only proceed if we found a token
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // validateToken() checks:
            // 1. Token signature matches our secret key
            // 2. Token has not expired
            // 3. Token is properly formatted
            
            // Extract the user's email from the token (stored in "subject" claim)
            String email = jwtTokenProvider.getSubject(token);
            
            // Load the full user object from MySQL database
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                // ============================================================
                // STEP 3: Create Spring Security Authentication Object
                // ============================================================
                
                // UsernamePasswordAuthenticationToken is Spring's way of representing
                // an authenticated user. Even though we're using JWT (not username/password),
                // we still use this class to hold the authentication info.
                //
                // Parameters:
                // 1. principal (email) - The user identifier
                // 2. credentials (null) - We don't need password here, token was validated
                // 3. authorities (empty list) - User roles/permissions (empty for now)
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
                
                // Add request details (IP address, session ID, etc.) for audit logging
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // ============================================================
                // STEP 4: Store Authentication in SecurityContext
                // ============================================================
                
                // SecurityContextHolder is a thread-local storage for the current user
                // After this line, Spring Security knows who the current user is
                // Controllers can access this via @AuthenticationPrincipal or SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // ====================================================================
        // STEP 5: Continue with Filter Chain
        // ====================================================================
        
        // IMPORTANT: Always call this, whether authenticated or not!
        // This passes the request to the next filter in the chain
        // If we don't call this, the request stops here and never reaches the controller
        filterChain.doFilter(request, response);
    }
}
