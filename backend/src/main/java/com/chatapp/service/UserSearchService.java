package com.chatapp.service;

import com.chatapp.dto.UserSearchResponse;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * USER SEARCH SERVICE - Handles user search operations
 * ============================================================================
 * 
 * Provides functionality to search for users by email or username.
 * Only returns safe user data (no passwords or sensitive info).
 * 
 * SECURITY:
 * ---------
 * - Only authenticated users can search
 * - Only active users are returned in results
 * - Passwords and sensitive data are never exposed
 */
public interface UserSearchService {
    
    /**
     * Searches for users by email (partial match).
     * 
     * @param email The email pattern to search for
     * @param currentUserId The ID of the user performing the search (excluded from results)
     * @return List of matching users
     */
    List<UserSearchResponse> searchByEmail(String email, Long currentUserId);
    
    /**
     * Searches for users by display name (partial match).
     * 
     * @param displayName The display name pattern to search for
     * @param currentUserId The ID of the user performing the search (excluded from results)
     * @return List of matching users
     */
    List<UserSearchResponse> searchByDisplayName(String displayName, Long currentUserId);
    
    /**
     * Searches for users by email or display name (partial match).
     * 
     * @param query The search query
     * @param currentUserId The ID of the user performing the search (excluded from results)
     * @return List of matching users
     */
    List<UserSearchResponse> search(String query, Long currentUserId);
}

/**
 * Implementation of UserSearchService.
 */
@Service
class UserSearchServiceImpl implements UserSearchService {

    private static final Logger logger = LoggerFactory.getLogger(UserSearchServiceImpl.class);

    private final UserRepository userRepository;

    public UserSearchServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserSearchResponse> searchByEmail(String email, Long currentUserId) {
        logger.debug("Searching users by email: {}", email);
        
        return userRepository.searchByEmail(email, currentUserId).stream()
            .filter(user -> "ACTIVE".equals(user.getStatus()))     // Only active users
            .map(UserSearchResponse::fromUser)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserSearchResponse> searchByDisplayName(String displayName, Long currentUserId) {
        logger.debug("Searching users by display name: {}", displayName);
        
        return userRepository.searchByDisplayName(displayName, currentUserId).stream()
            .filter(user -> "ACTIVE".equals(user.getStatus()))     // Only active users
            .map(UserSearchResponse::fromUser)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserSearchResponse> search(String query, Long currentUserId) {
        logger.debug("Searching users by query: {}", query);
        
        return userRepository.searchByEmailOrDisplayName(query, currentUserId).stream()
            .filter(user -> "ACTIVE".equals(user.getStatus()))     // Only active users
            .map(UserSearchResponse::fromUser)
            .collect(Collectors.toList());
    }
}
