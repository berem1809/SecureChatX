package com.chatapp.dto;

import com.chatapp.model.Conversation;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CONVERSATION RESPONSE DTO
 * ============================================================================
 * 
 * Response object for conversation details.
 * Used when returning conversation information to clients.
 */
public class ConversationResponse {

    private Long id;
    private UserSearchResponse user1;
    private UserSearchResponse user2;
    private UserSearchResponse otherParticipant;  // Convenience field for the client
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ConversationResponse() {}

    // ========================================================================
    // STATIC FACTORY METHODS
    // ========================================================================

    /**
     * Creates a ConversationResponse from a Conversation entity.
     * 
     * @param conversation The conversation entity
     * @return ConversationResponse with all relevant data
     */
    public static ConversationResponse fromEntity(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUser1(UserSearchResponse.fromUser(conversation.getUser1()));
        response.setUser2(UserSearchResponse.fromUser(conversation.getUser2()));
        response.setCreatedAt(conversation.getCreatedAt());
        response.setLastMessageAt(conversation.getLastMessageAt());
        return response;
    }

    /**
     * Creates a ConversationResponse from a Conversation entity,
     * with the other participant pre-calculated for convenience.
     * 
     * @param conversation The conversation entity
     * @param currentUserId The ID of the current user
     * @return ConversationResponse with otherParticipant set
     */
    public static ConversationResponse fromEntityWithCurrentUser(Conversation conversation, Long currentUserId) {
        ConversationResponse response = fromEntity(conversation);
        
        // Set the other participant for client convenience
        if (conversation.getUser1().getId().equals(currentUserId)) {
            response.setOtherParticipant(UserSearchResponse.fromUser(conversation.getUser2()));
        } else {
            response.setOtherParticipant(UserSearchResponse.fromUser(conversation.getUser1()));
        }
        
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSearchResponse getUser1() { return user1; }
    public void setUser1(UserSearchResponse user1) { this.user1 = user1; }

    public UserSearchResponse getUser2() { return user2; }
    public void setUser2(UserSearchResponse user2) { this.user2 = user2; }

    public UserSearchResponse getOtherParticipant() { return otherParticipant; }
    public void setOtherParticipant(UserSearchResponse otherParticipant) { this.otherParticipant = otherParticipant; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
