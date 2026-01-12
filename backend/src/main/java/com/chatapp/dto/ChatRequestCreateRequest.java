package com.chatapp.dto;

import jakarta.validation.constraints.Email;

/**
 * ============================================================================
 * CHAT REQUEST CREATE REQUEST DTO
 * ============================================================================
 * 
 * Request object for sending a chat request to another user.
 * Either receiverId OR receiverEmail must be provided.
 * 
 * NOTE: The sender is determined from the JWT token, not from this request.
 * This prevents users from impersonating others.
 */
public class ChatRequestCreateRequest {

    /**
     * ID of the user to send the chat request to.
     * The sender is extracted from JWT token.
     * Either receiverId or receiverEmail must be provided.
     */
    private Long receiverId;

    /**
     * Email of the user to send the chat request to.
     * Alternative to receiverId - the system will look up the user.
     */
    @Email(message = "Invalid email format")
    private String receiverEmail;

    /**
     * Optional message to include with the request.
     * Example: "Hi, I'd like to connect!"
     */
    private String message;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ChatRequestCreateRequest() {}

    public ChatRequestCreateRequest(Long receiverId) {
        this.receiverId = receiverId;
    }

    public ChatRequestCreateRequest(Long receiverId, String message) {
        this.receiverId = receiverId;
        this.message = message;
    }

    public static ChatRequestCreateRequest withEmail(String receiverEmail) {
        ChatRequestCreateRequest request = new ChatRequestCreateRequest();
        request.setReceiverEmail(receiverEmail);
        return request;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    /**
     * Check if either receiverId or receiverEmail is provided.
     */
    public boolean hasReceiver() {
        return receiverId != null || (receiverEmail != null && !receiverEmail.isBlank());
    }
}
