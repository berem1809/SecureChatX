package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * CONVERSATION ENTITY - Represents a one-to-one conversation
 * ============================================================================
 * 
 * WHAT IS A CONVERSATION?
 * -----------------------
 * A conversation is a chat channel between exactly two users.
 * It's created only after one user accepts a chat request from another.
 * 
 * IMPORTANT DESIGN DECISIONS:
 * ---------------------------
 * 1. USER ID ORDERING: user1Id is always less than user2Id
 *    This ensures the same pair of users always maps to the same conversation.
 *    Example: Users 5 and 3 → user1Id=3, user2Id=5
 * 
 * 2. NO DIRECT CONVERSATION CREATION:
 *    Conversations are created only through ChatRequest acceptance.
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE conversations (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     user1_id BIGINT NOT NULL REFERENCES users(id),
 *     user2_id BIGINT NOT NULL REFERENCES users(id),
 *     created_at TIMESTAMP NOT NULL,
 *     last_message_at TIMESTAMP,
 *     UNIQUE(user1_id, user2_id)  -- Prevent duplicate conversations
 * );
 * 
 * @see ConversationService For business logic
 * @see ChatRequestService Creates conversations on request acceptance
 */
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user1_id", "user2_id"},
        name = "uk_conversation_users"
    ),
    indexes = {
        @Index(name = "idx_conversation_user1", columnList = "user1_id"),
        @Index(name = "idx_conversation_user2", columnList = "user2_id"),
        @Index(name = "idx_conversation_last_message", columnList = "last_message_at")
    }
)
public class Conversation {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // PARTICIPANTS
    // ========================================================================

    /**
     * First participant (with smaller user ID).
     * CONVENTION: user1Id < user2Id always
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /**
     * Second participant (with larger user ID).
     * CONVENTION: user1Id < user2Id always
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last message in this conversation.
     * Used for sorting conversations by recent activity.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // ========================================================================
    // MEMBERS (for future extensibility - currently just the two users)
    // ========================================================================

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationMember> members = new ArrayList<>();

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public Conversation() {}

    /**
     * Creates a new conversation between two users.
     * Automatically orders users so user1Id < user2Id.
     * 
     * @param userA First user
     * @param userB Second user
     */
    public Conversation(User userA, User userB) {
        // Ensure user1.id < user2.id for consistent ordering
        if (userA.getId() < userB.getId()) {
            this.user1 = userA;
            this.user2 = userB;
        } else {
            this.user1 = userB;
            this.user2 = userA;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Checks if a user is a participant in this conversation.
     * 
     * @param userId The user ID to check
     * @return true if user is a participant
     */
    public boolean hasParticipant(Long userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }

    /**
     * Gets the other participant in the conversation.
     * 
     * @param userId The current user's ID
     * @return The other user, or null if userId is not a participant
     */
    public User getOtherParticipant(Long userId) {
        if (user1.getId().equals(userId)) {
            return user2;
        } else if (user2.getId().equals(userId)) {
            return user1;
        }
        return null;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser1() { return user1; }
    public void setUser1(User user1) { this.user1 = user1; }

    public User getUser2() { return user2; }
    public void setUser2(User user2) { this.user2 = user2; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public List<ConversationMember> getMembers() { return members; }
    public void setMembers(List<ConversationMember> members) { this.members = members; }
}
