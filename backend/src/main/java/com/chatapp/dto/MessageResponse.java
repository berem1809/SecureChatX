package com.chatapp.dto;

import com.chatapp.model.Message;
import java.time.LocalDateTime;

/**
 * DTO for message responses - Returns encrypted messages to clients.
 * 
 * IMPORTANT: 
 * - `content` field contains ENCRYPTED content (Base64)
 * - Server returns encryptedContent and nonce
 * - Frontend uses senderPublicKey to derive shared secret and decrypt
 * - Server NEVER returns plaintext
 */
public class MessageResponse {

    private Long id;
    private Long conversationId;  // Contains either conversation ID or group ID
    private Long senderId;
    private String senderUsername;
    private String senderEmail;
    private String senderDisplayName;
    
    // Message type identification
    private Boolean isGroup;      // true if this is a group message, false for direct message
    private Long groupId;          // Only set if isGroup = true
    
    // For plain-text messages (deprecated, use encryptedContent instead)
    private String content;
    
    // Encryption fields (RECOMMENDED)
    private Boolean isEncrypted;
    private String encryptedContent;  // Base64 encoded XSalsa20-Poly1305 ciphertext
    private String encryptionNonce;   // Base64 encoded 24-byte nonce
    private String senderPublicKey;   // Base64 encoded X25519 public key
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MessageResponse() {}

    /**
     * Create response from Message entity
     * Includes encrypted content fields
     * Handles both GROUP and DIRECT messages
     */
    public static MessageResponse fromEntity(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        
        // Determine if this is a group message or direct message
        if (message.getGroup() != null) {
            // GROUP MESSAGE
            response.setIsGroup(true);
            response.setGroupId(message.getGroup().getId());
            response.setConversationId(message.getGroup().getId());  // For backward compatibility
        } else if (message.getConversation() != null) {
            // DIRECT MESSAGE
            response.setIsGroup(false);
            response.setGroupId(null);
            response.setConversationId(message.getConversation().getId());
        } else {
            // Should never happen, but handle gracefully
            throw new IllegalStateException("Message must belong to either a conversation or a group");
        }
        
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getEmail());
        response.setSenderEmail(message.getSender().getEmail());
        response.setSenderDisplayName(message.getSender().getDisplayName());
        
        // Plain text (deprecated)
        response.setContent(message.getContent());
        
        // Encryption fields
        response.setIsEncrypted(message.getIsEncrypted());
        response.setEncryptedContent(message.getEncryptedContent());
        response.setEncryptionNonce(message.getEncryptionNonce());
        response.setSenderPublicKey(message.getSenderPublicKey());
        
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderDisplayName() { return senderDisplayName; }
    public void setSenderDisplayName(String senderDisplayName) { this.senderDisplayName = senderDisplayName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getEncryptionNonce() { return encryptionNonce; }
    public void setEncryptionNonce(String encryptionNonce) { this.encryptionNonce = encryptionNonce; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }
    
    public Boolean getIsGroup() { return isGroup; }
    public void setIsGroup(Boolean isGroup) { this.isGroup = isGroup; }
    
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    
    // Deprecated field names (for backward compatibility)
    public String getCiphertext() { return encryptedContent; }
    public void setCiphertext(String ciphertext) { this.encryptedContent = ciphertext; }

    public String getNonce() { return encryptionNonce; }
    public void setNonce(String nonce) { this.encryptionNonce = nonce; }
}

