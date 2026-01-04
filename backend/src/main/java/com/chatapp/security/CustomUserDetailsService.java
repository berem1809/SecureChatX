package com.chatapp.security;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.User;  // Our JPA User entity
import com.chatapp.repository.UserRepository;  // For database access

// Spring Security classes for authentication
import org.springframework.security.core.userdetails.UserDetails;  // Security user interface
import org.springframework.security.core.userdetails.UserDetailsService;  // Service interface
import org.springframework.security.core.userdetails.UsernameNotFoundException;  // Exception for user not found

import org.springframework.stereotype.Service;  // Marks this as a Spring service

import java.util.Collections;  // For empty authority list

/**
 * ============================================================================
 * CUSTOM USER DETAILS SERVICE - Loads users for Spring Security
 * ============================================================================
 * 
 * WHAT IS UserDetailsService?
 * ---------------------------
 * UserDetailsService is a core Spring Security interface with ONE method:
 *   loadUserByUsername(String username) → UserDetails
 * 
 * Spring Security calls this during authentication to load user data from
 * your database. You must provide this implementation to tell Spring Security
 * how to find users in YOUR specific database.
 * 
 * HOW IT FITS IN AUTHENTICATION FLOW:
 * -----------------------------------
 * 
 * 1. User submits login form with email + password
 *    ↓
 * 2. Spring Security calls loadUserByUsername(email)
 *    ↓
 * 3. This service queries MySQL via UserRepository
 *    ↓
 * 4. Returns UserDetails object with email, hashed password, and authorities
 *    ↓
 * 5. Spring Security compares submitted password with stored hash
 *    ↓
 * 6. If match → Authentication successful!
 * 
 * WHY WE NEED THIS:
 * -----------------
 * - Spring Security doesn't know about our User entity
 * - We must translate our User → Spring's UserDetails
 * - This "bridges" our database model to Spring Security's model
 * 
 * WHAT IS UserDetails?
 * --------------------
 * UserDetails is Spring Security's representation of a user:
 * - getUsername() → email
 * - getPassword() → hashed password
 * - getAuthorities() → roles/permissions (ROLE_USER, ROLE_ADMIN, etc.)
 * - isEnabled(), isAccountNonLocked(), etc. → account status checks
 * 
 * NOTE: In our app, we handle status checks (ACTIVE vs PENDING) in AuthService,
 * not here. This service just loads the raw user data.
 * 
 * @see JwtAuthenticationFilter Uses this indirectly via Spring Security
 * @see SecurityConfig Where this service is registered
 */
@Service  // Marks this as a Spring-managed service bean
public class CustomUserDetailsService implements UserDetailsService {
    
    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    
    /**
     * Repository for accessing User data in MySQL.
     * Injected by Spring's dependency injection (constructor injection).
     */
    private final UserRepository userRepository;

    /**
     * Constructor injection - Spring automatically provides UserRepository.
     * 
     * WHY CONSTRUCTOR INJECTION?
     * - Makes dependencies explicit and required
     * - Enables easy testing (can mock UserRepository)
     * - Fields can be final (immutable)
     * 
     * @param userRepository Repository for User database operations
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ========================================================================
    // SPRING SECURITY INTERFACE METHOD
    // ========================================================================

    /**
     * Loads user by their username (email in our case).
     * 
     * CALLED BY SPRING SECURITY:
     * - During form login authentication
     * - When validating JWT tokens (to ensure user still exists)
     * - Anywhere Spring Security needs to look up a user
     * 
     * @param username The username to search for (we use email as username)
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException If no user found with this email
     * 
     * FLOW:
     * 1. Query MySQL for user with this email
     * 2. If not found → throw UsernameNotFoundException
     * 3. If found → convert our User to Spring's UserDetails
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        // Step 1: Query database for user by email
        // We use email as the "username" for login
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Step 2: Convert our User entity to Spring Security's UserDetails
        // Using Spring Security's built-in User builder for convenience
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())     // Set username (email)
                .password(user.getPassword())       // Set hashed password (for comparison)
                .authorities(Collections.emptyList())  // Empty authorities (we handle roles in JWT)
                .build();
        
        /*
         * WHY EMPTY AUTHORITIES?
         * ----------------------
         * We store roles in the JWT token claims and extract them in
         * JwtAuthenticationFilter. So we don't need to load authorities here.
         * 
         * If you wanted to include roles from database:
         * .authorities(user.getRoles().stream()
         *     .map(role -> new SimpleGrantedAuthority(role))
         *     .collect(Collectors.toList()))
         */
    }
}
