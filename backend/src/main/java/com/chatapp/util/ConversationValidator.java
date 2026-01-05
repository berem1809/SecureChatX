package com.chatapp.util;

import com.chatapp.exception.ConversationAccessDeniedException;
import com.chatapp.exception.ConversationNotFoundException;
import com.chatapp.model.Conversation;
import com.chatapp.repository.ConversationRepository;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * CONVERSATION VALIDATOR
 * ============================================================================
 * 
 * Utility class for validating conversation access and membership.
 * Used by service layer to enforce authorization rules.
 * 
 * AUTHORIZATION RULES:
 * --------------------
 * 1. Only conversation participants can access the conversation
 * 2. User ID is extracted from JWT token, not from request body
 * 3. All access checks throw appropriate exceptions if failed
 */
@Component
public class ConversationValidator {

    private final ConversationRepository conversationRepository;

    public ConversationValidator(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    /**
     * Validates that a user is a participant of a conversation.
     * 
     * @param conversationId The conversation ID
     * @param userId The user ID to validate
     * @return The Conversation if user is a participant
     * @throws ConversationNotFoundException if conversation doesn't exist
     * @throws ConversationAccessDeniedException if user is not a participant
     */
    public Conversation validateParticipant(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (!conversation.hasParticipant(userId)) {
            throw new ConversationAccessDeniedException(conversationId, userId);
        }
        
        return conversation;
    }

    /**
     * Validates that a user is a participant and returns the conversation.
     * Uses the optimized query that checks both conditions at once.
     * 
     * @param conversationId The conversation ID
     * @param userId The user ID to validate
     * @return The Conversation if user is a participant
     * @throws ConversationAccessDeniedException if user is not a participant or conversation doesn't exist
     */
    public Conversation validateAndGetConversation(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ConversationAccessDeniedException(conversationId, userId));
    }

    /**
     * Checks if a conversation exists between two users.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return true if a conversation exists
     */
    public boolean conversationExists(Long user1Id, Long user2Id) {
        return conversationRepository.existsBetweenUsers(user1Id, user2Id);
    }

    /**
     * Checks if a user is a participant of a conversation.
     * 
     * @param conversationId The conversation ID
     * @param userId The user ID
     * @return true if user is a participant
     */
    public boolean isParticipant(Long conversationId, Long userId) {
        return conversationRepository.findById(conversationId)
            .map(conv -> conv.hasParticipant(userId))
            .orElse(false);
    }
}
