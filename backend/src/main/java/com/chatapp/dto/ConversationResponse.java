package com.chatapp.dto;

import com.chatapp.model.Conversation;
import com.chatapp.model.Group;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CONVERSATION RESPONSE DTO
 * ============================================================================
 * 
 * Response object for conversation details.
 * Used when returning conversation information to clients.
 * Supports both direct (1-to-1) and group conversations.
 */
public class ConversationResponse {

    private Long id;
    private String name;                       // For groups: group name, for direct: null
    private Boolean isGroup;                   // true for groups, false for direct chats
    private UserSearchResponse user1;          // For direct chats only
    private UserSearchResponse user2;          // For direct chats only
    private UserSearchResponse otherParticipant;  // Convenience field for the client
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;                  // Number of unread messages for current user

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ConversationResponse() {}

    // ========================================================================
    // STATIC FACTORY METHODS
    // ========================================================================

    /**
     * Creates a ConversationResponse from a Conversation entity (direct chat).
     * 
     * @param conversation The conversation entity
     * @return ConversationResponse with all relevant data
     */
    public static ConversationResponse fromEntity(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setIsGroup(false);
        response.setName(null);
        response.setUser1(UserSearchResponse.fromUser(conversation.getUser1()));
        response.setUser2(UserSearchResponse.fromUser(conversation.getUser2()));
        response.setCreatedAt(conversation.getCreatedAt());
        response.setLastMessageAt(conversation.getLastMessageAt());
        return response;
    }

    /**
     * Creates a ConversationResponse from a Conversation entity (direct chat),
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

    /**
     * Creates a ConversationResponse from a Group entity (group chat).
     * 
     * @param group The group entity
     * @return ConversationResponse representing the group conversation
     */
    public static ConversationResponse fromGroup(Group group) {
        ConversationResponse response = new ConversationResponse();
        response.setId(group.getId());
        response.setIsGroup(true);
        response.setName(group.getName());
        response.setCreatedAt(group.getCreatedAt());
        response.setLastMessageAt(group.getCreatedAt());  // Use group creation time as placeholder
        response.setUser1(null);
        response.setUser2(null);
        response.setOtherParticipant(null);
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsGroup() { return isGroup; }
    public void setIsGroup(Boolean isGroup) { this.isGroup = isGroup; }

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

    public Long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
}
