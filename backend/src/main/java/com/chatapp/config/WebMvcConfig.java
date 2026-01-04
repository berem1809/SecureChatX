package com.chatapp.config;

// ============================================================================
// IMPORTS
// ============================================================================

// Spring Framework annotations
import org.springframework.context.annotation.Configuration;  // Marks this as a config class

// Spring Web MVC imports
import org.springframework.web.servlet.config.annotation.CorsRegistry;       // For registering CORS mappings
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;   // Interface to customize MVC config

/**
 * ============================================================================
 * WEB MVC CONFIGURATION - CORS (Cross-Origin Resource Sharing) Settings
 * ============================================================================
 * 
 * WHAT IS CORS?
 * -------------
 * CORS is a security feature built into web browsers that blocks web pages
 * from making requests to a different domain than the one that served the page.
 * 
 * THE PROBLEM CORS SOLVES:
 * ------------------------
 * Imagine a malicious website trying to steal your data:
 * 
 * 1. You're logged into your-bank.com (have auth cookies)
 * 2. You visit evil-site.com
 * 3. Evil site's JavaScript tries to call your-bank.com/api/transfer-money
 * 4. WITHOUT CORS: Browser sends request WITH your auth cookies → Money stolen!
 * 5. WITH CORS: Browser blocks the request → You're safe!
 * 
 * WHY WE NEED TO CONFIGURE CORS:
 * ------------------------------
 * In development, our setup is:
 * - Frontend (React): http://localhost:3000
 * - Backend (Spring): http://localhost:8080
 * 
 * These are DIFFERENT ORIGINS (different ports), so browsers block requests by default.
 * We need to tell the backend: "It's OK, allow requests from localhost:3000"
 * 
 * WHAT IS AN "ORIGIN"?
 * --------------------
 * An origin is: Protocol + Domain + Port
 * 
 * Examples:
 * - http://localhost:3000  (React dev server)
 * - http://localhost:8080  (Spring Boot server)
 * - https://myapp.com      (Production frontend)
 * - https://api.myapp.com  (Production backend)
 * 
 * Same origin: http://localhost:3000/page1 and http://localhost:3000/page2
 * Different origin: http://localhost:3000 and http://localhost:8080 (different port!)
 * 
 * HOW CORS WORKS (Simplified):
 * ----------------------------
 * 
 * 1. SIMPLE REQUESTS (GET, POST with simple headers):
 *    Browser                              Server
 *      │                                    │
 *      │── GET /api/data ─────────────────→│
 *      │   Origin: http://localhost:3000   │
 *      │                                    │
 *      │←── Response ──────────────────────│
 *      │   Access-Control-Allow-Origin:    │
 *      │   http://localhost:3000           │
 *    
 *    Browser checks: Does Allow-Origin match my origin? 
 *    If YES → Allow JavaScript to see response
 *    If NO → Block and throw CORS error
 * 
 * 2. PREFLIGHT REQUESTS (PUT, DELETE, custom headers):
 *    Browser                              Server
 *      │                                    │
 *      │── OPTIONS /api/data (preflight) ─→│  "Can I make this request?"
 *      │   Origin: http://localhost:3000   │
 *      │   Access-Control-Request-Method:  │
 *      │   DELETE                          │
 *      │                                    │
 *      │←── Preflight Response ────────────│  "Yes, here's what's allowed"
 *      │   Access-Control-Allow-Origin:    │
 *      │   http://localhost:3000           │
 *      │   Access-Control-Allow-Methods:   │
 *      │   GET, POST, DELETE               │
 *      │                                    │
 *      │── DELETE /api/data ──────────────→│  "OK, now I'll make the real request"
 *      │                                    │
 *      │←── Response ──────────────────────│
 * 
 * COMMON CORS ERRORS:
 * -------------------
 * Error: "Access to XMLHttpRequest at 'http://localhost:8080/api/...' from origin 
 *        'http://localhost:3000' has been blocked by CORS policy"
 * 
 * Fix: Make sure the backend's CORS config includes the frontend's origin.
 * 
 * PRODUCTION CONSIDERATIONS:
 * --------------------------
 * In production, you should:
 * 1. Replace "http://localhost:3000" with your actual frontend domain
 * 2. Consider using environment variables for the allowed origins
 * 3. Be specific about allowed methods (don't just allow everything)
 * 4. Consider if you really need allowCredentials(true)
 * 
 * @see SecurityConfig Where CORS is enabled in the security filter chain
 */
@Configuration  // Marks this class as a Spring configuration class
public class WebMvcConfig implements WebMvcConfigurer {
    // WebMvcConfigurer is an interface that lets us customize Spring MVC behavior.
    // By implementing it, we can override methods to add our own configuration.

    /**
     * Configures CORS (Cross-Origin Resource Sharing) mappings.
     * 
     * This method is called by Spring during startup to register CORS rules.
     * 
     * @param registry CorsRegistry to register CORS configurations
     */
    @Override  // We're overriding the default implementation from WebMvcConfigurer
    public void addCorsMappings(CorsRegistry registry) {
        
        // ====================================================================
        // CORS CONFIGURATION
        // ====================================================================
        
        registry
            // ------------------------------------------------------------
            // PATH PATTERN: Which endpoints this CORS config applies to
            // ------------------------------------------------------------
            // "/api/**" means:
            //   - /api/auth/login       ✓ Matches
            //   - /api/users/123        ✓ Matches
            //   - /api/messages/all     ✓ Matches
            //   - /other/endpoint       ✗ Does NOT match
            //
            // The "**" is a wildcard that matches any path segments
            .addMapping("/api/**")
            
            // ------------------------------------------------------------
            // ALLOWED ORIGINS: Which domains can make requests
            // ------------------------------------------------------------
            // This is the frontend's URL. In development, React runs on port 3000.
            //
            // SECURITY WARNING: Be specific here!
            // - DON'T use "*" (allows any origin) when allowCredentials is true
            // - DO list specific origins you trust
            //
            // For production, change this to your actual frontend domain:
            //   .allowedOrigins("https://myapp.com", "https://www.myapp.com")
            //
            // For multiple origins (dev + production):
            //   .allowedOrigins("http://localhost:3000", "https://myapp.com")
            .allowedOrigins("http://localhost:3000")
            
            // ------------------------------------------------------------
            // ALLOWED METHODS: Which HTTP methods are permitted
            // ------------------------------------------------------------
            // These are the HTTP methods the frontend can use:
            //
            // GET     - Retrieve data (e.g., fetch messages)
            // POST    - Create data (e.g., send message, login)
            // PUT     - Update data (e.g., edit message)
            // DELETE  - Delete data (e.g., delete message)
            // OPTIONS - Preflight requests (browser sends automatically)
            //
            // Note: OPTIONS is required for preflight requests!
            // Browser sends OPTIONS before PUT/DELETE to check if allowed.
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            
            // ------------------------------------------------------------
            // ALLOWED HEADERS: Which headers the frontend can send
            // ------------------------------------------------------------
            // "*" allows any header. Common headers include:
            //
            // - Content-Type: application/json (for JSON requests)
            // - Authorization: Bearer <token> (for JWT authentication)
            // - Accept: application/json (expected response type)
            //
            // For more security, you could list specific headers:
            //   .allowedHeaders("Content-Type", "Authorization", "Accept")
            .allowedHeaders("*")
            
            // ------------------------------------------------------------
            // ALLOW CREDENTIALS: Allow cookies/auth headers
            // ------------------------------------------------------------
            // When true, the browser will:
            // - Send cookies with cross-origin requests
            // - Send Authorization headers
            //
            // We need this because:
            // - Our refresh token is stored in an HttpOnly cookie
            // - The cookie needs to be sent with /api/auth/refresh requests
            //
            // IMPORTANT: When allowCredentials is true:
            // - allowedOrigins CANNOT be "*" (must be specific)
            // - The response will include: Access-Control-Allow-Credentials: true
            .allowCredentials(true);
            
            // ------------------------------------------------------------
            // OTHER OPTIONS (not used here, but good to know):
            // ------------------------------------------------------------
            //
            // .exposedHeaders("X-Custom-Header")
            //   → Which headers the frontend JS can read from response
            //   → By default, only simple headers are exposed
            //
            // .maxAge(3600)
            //   → How long (seconds) browser caches preflight response
            //   → Reduces OPTIONS requests for repeated calls
    }
}