package com.chatapp.dto;

import com.chatapp.model.ChatRequest;
import com.chatapp.model.ChatRequestStatus;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CHAT REQUEST RESPONSE DTO
 * ============================================================================
 * 
 * Response object containing chat request details.
 * Used when returning chat request information to clients.
 */
public class ChatRequestResponse {

    private Long id;
    private UserSearchResponse sender;
    private UserSearchResponse receiver;
    private ChatRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ChatRequestResponse() {}

    // ========================================================================
    // STATIC FACTORY METHOD
    // ========================================================================

    /**
     * Creates a ChatRequestResponse from a ChatRequest entity.
     * 
     * @param chatRequest The chat request entity
     * @return ChatRequestResponse with all relevant data
     */
    public static ChatRequestResponse fromEntity(ChatRequest chatRequest) {
        ChatRequestResponse response = new ChatRequestResponse();
        response.setId(chatRequest.getId());
        response.setSender(UserSearchResponse.fromUser(chatRequest.getSender()));
        response.setReceiver(UserSearchResponse.fromUser(chatRequest.getReceiver()));
        response.setStatus(chatRequest.getStatus());
        response.setCreatedAt(chatRequest.getCreatedAt());
        response.setUpdatedAt(chatRequest.getUpdatedAt());
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSearchResponse getSender() { return sender; }
    public void setSender(UserSearchResponse sender) { this.sender = sender; }

    public UserSearchResponse getReceiver() { return receiver; }
    public void setReceiver(UserSearchResponse receiver) { this.receiver = receiver; }

    public ChatRequestStatus getStatus() { return status; }
    public void setStatus(ChatRequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
