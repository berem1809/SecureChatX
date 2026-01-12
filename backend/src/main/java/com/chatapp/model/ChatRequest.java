package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * CHAT REQUEST ENTITY - Represents a chat request between two users
 * ============================================================================
 * 
 * WHAT IS A CHAT REQUEST?
 * -----------------------
 * Before two users can have a conversation, one user must send a chat request
 * to the other. This prevents unwanted messages and spam.
 * 
 * WORKFLOW:
 * ---------
 * 1. User A sends chat request to User B → Status: PENDING
 * 2. User B accepts the request → Status: ACCEPTED → Conversation created
 *    OR
 * 2. User B rejects the request → Status: REJECTED → No conversation
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE chat_requests (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     sender_id BIGINT NOT NULL REFERENCES users(id),
 *     receiver_id BIGINT NOT NULL REFERENCES users(id),
 *     status VARCHAR(20) NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP,
 *     UNIQUE(sender_id, receiver_id)  -- Prevent duplicate requests
 * );
 * 
 * @see ChatRequestService For business logic
 * @see ChatRequestController For REST endpoints
 */
@Entity
@Table(
    name = "chat_requests",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"sender_id", "receiver_id"},
        name = "uk_chat_request_sender_receiver"
    ),
    indexes = {
        @Index(name = "idx_chat_request_sender", columnList = "sender_id"),
        @Index(name = "idx_chat_request_receiver", columnList = "receiver_id"),
        @Index(name = "idx_chat_request_status", columnList = "status")
    }
)
public class ChatRequest {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * The user who sent the chat request.
     * EAGER fetch because we always need sender info when loading request.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The user who receives the chat request.
     * EAGER fetch because we always need receiver info when loading request.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // ========================================================================
    // STATUS
    // ========================================================================

    /**
     * Current status of the chat request.
     * Values: PENDING, ACCEPTED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRequestStatus status = ChatRequestStatus.PENDING;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ChatRequest() {}

    public ChatRequest(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = ChatRequestStatus.PENDING;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public ChatRequestStatus getStatus() { return status; }
    public void setStatus(ChatRequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
