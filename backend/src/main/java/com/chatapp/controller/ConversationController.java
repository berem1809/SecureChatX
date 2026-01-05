package com.chatapp.controller;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * CONVERSATION CONTROLLER - Handles conversation endpoints
 * ============================================================================
 * 
 * REST Endpoints:
 * ---------------
 * GET /api/conversations                     - Get all user's conversations
 * GET /api/conversations/{id}                - Get specific conversation
 * GET /api/conversations/with/{userId}       - Get conversation with specific user
 * 
 * SECURITY:
 * ---------
 * - All endpoints require valid JWT token
 * - User can only access conversations they are a participant of
 * - Conversations are created only through chat request acceptance
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    /**
     * Gets all conversations for the current user.
     * Sorted by last message time (most recent first).
     * 
     * GET /api/conversations
     * 
     * @param authentication The authentication object
     * @return List of conversations
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getUserConversations(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting conversations for user {}", userId);
        
        List<ConversationResponse> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Gets a specific conversation by ID.
     * 
     * GET /api/conversations/{conversationId}
     * 
     * @param conversationId The conversation ID
     * @param authentication The authentication object
     * @return The conversation details
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting conversation {} for user {}", conversationId, userId);
        
        ConversationResponse conversation = conversationService.getConversation(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Gets a conversation with a specific user.
     * 
     * GET /api/conversations/with/{otherUserId}
     * 
     * @param otherUserId The other user's ID
     * @param authentication The authentication object
     * @return The conversation, or 404 if not found
     */
    @GetMapping("/with/{otherUserId}")
    public ResponseEntity<ConversationResponse> getConversationWithUser(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting conversation between user {} and user {}", userId, otherUserId);
        
        ConversationResponse conversation = conversationService.getConversationBetweenUsers(userId, otherUserId);
        
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(conversation);
    }

    /**
     * Checks if a conversation exists with a specific user.
     * 
     * GET /api/conversations/exists/{otherUserId}
     * 
     * @param otherUserId The other user's ID
     * @param authentication The authentication object
     * @return true if conversation exists
     */
    @GetMapping("/exists/{otherUserId}")
    public ResponseEntity<Boolean> conversationExists(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Checking if conversation exists between user {} and user {}", userId, otherUserId);
        
        boolean exists = conversationService.conversationExists(userId, otherUserId);
        return ResponseEntity.ok(exists);
    }

    /**
     * Extracts the user ID from the authentication object.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }
}
