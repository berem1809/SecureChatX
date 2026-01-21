package com.chatapp.controller;

import com.chatapp.dto.UserSearchResponse;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * USER CONTROLLER - Handles user search and profile endpoints
 * ============================================================================
 * 
 * REST Endpoints:
 * ---------------
 * GET /api/users/search?q={query}      - Search users by email or display name
 * GET /api/users/search/email?q={email} - Search users by email
 * GET /api/users/search/name?q={name}   - Search users by display name
 * GET /api/users/me                     - Get current user's profile
 * 
 * SECURITY:
 * ---------
 * - All endpoints require valid JWT token
 * - User ID is extracted from JWT, not from request
 * - Only active users are returned in search results
 * - Passwords and sensitive data are never exposed
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserSearchService userSearchService;
    private final UserRepository userRepository;

    public UserController(UserSearchService userSearchService, UserRepository userRepository) {
        this.userSearchService = userSearchService;
        this.userRepository = userRepository;
    }

    /**
     * Gets the current user's profile.
     * 
     * GET /api/users/me
     * 
     * @param authentication The authentication object containing user details
     * @return Current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserSearchResponse> getCurrentUser(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting profile for user {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(UserSearchResponse.fromUser(user));
    }

    /**
     * Searches for users by email or display name.
     * 
     * GET /api/users/search?q={query}
     * 
     * @param query The search query
     * @param authentication The authentication object
     * @return List of matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam("q") String query,
            Authentication authentication) {
        
        Long currentUserId = getUserIdFromAuth(authentication);
        logger.info("User {} searching for: {}", currentUserId, query);
        
        List<UserSearchResponse> results = userSearchService.search(query, currentUserId);
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for users by email.
     * 
     * GET /api/users/search/email?q={email}
     * 
     * @param email The email pattern to search
     * @param authentication The authentication object
     * @return List of matching users
     */
    @GetMapping("/search/email")
    public ResponseEntity<List<UserSearchResponse>> searchByEmail(
            @RequestParam("q") String email,
            Authentication authentication) {
        
        Long currentUserId = getUserIdFromAuth(authentication);
        logger.info("User {} searching by email: {}", currentUserId, email);
        
        List<UserSearchResponse> results = userSearchService.searchByEmail(email, currentUserId);
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for users by display name.
     * 
     * GET /api/users/search/name?q={name}
     * 
     * @param name The display name pattern to search
     * @param authentication The authentication object
     * @return List of matching users
     */
    @GetMapping("/search/name")
    public ResponseEntity<List<UserSearchResponse>> searchByDisplayName(
            @RequestParam("q") String name,
            Authentication authentication) {
        
        Long currentUserId = getUserIdFromAuth(authentication);
        logger.info("User {} searching by name: {}", currentUserId, name);
        
        List<UserSearchResponse> results = userSearchService.searchByDisplayName(name, currentUserId);
        return ResponseEntity.ok(results);
    }

    /**
     * Gets all available users (excluding current user).
     * 
     * GET /api/users/all
     * 
     * @param authentication The authentication object
     * @return List of all available users
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserSearchResponse>> getAllUsers(Authentication authentication) {
        Long currentUserId = getUserIdFromAuth(authentication);
        logger.info("User {} requesting all available users", currentUserId);
        
        List<UserSearchResponse> results = userSearchService.getAllUsers(currentUserId);
        return ResponseEntity.ok(results);
    }

    /**
     * Extracts the user ID from the authentication object.
     * The JWT filter sets the user email as the principal name.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }
}
