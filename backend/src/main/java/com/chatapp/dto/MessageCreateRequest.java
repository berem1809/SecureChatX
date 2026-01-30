package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for sending a new encrypted message.
 * 
 * ENCRYPTION FLOW:
 * 1. Frontend encrypts plaintext using XSalsa20-Poly1305
 * 2. Frontend sends encryptedContent + encryptionNonce + senderPublicKey
 * 3. Backend stores encrypted message (NEVER decrypts)
 * 4. Frontend (recipient) retrieves and decrypts using shared secret
 * 
 * FIELDS:
 * - content: Plain text (deprecated, use encryptedContent instead)
 * - encryptedContent: Base64 encoded XSalsa20-Poly1305 ciphertext (REQUIRED)
 * - encryptionNonce: Base64 encoded 24-byte nonce (REQUIRED)
 * - senderPublicKey: Base64 encoded X25519 public key (optional, backend retrieves from DB)
 */
public class MessageCreateRequest {

    // Plain text (deprecated - optional for encrypted messages)
    private String content;

    // Encrypted content (REQUIRED for encrypted messages)
    @NotBlank(message = "Encrypted content cannot be empty")
    private String encryptedContent;
    
    @NotBlank(message = "Encryption nonce cannot be empty")
    private String encryptionNonce;
    
    // Optional: Sender's public key (backend can retrieve from DB if not provided)
    private String senderPublicKey;
    
    // Flag indicating if message is encrypted
    private Boolean isEncrypted;

    public MessageCreateRequest() {}

    public MessageCreateRequest(String content) {
        this.content = content;
    }

    public MessageCreateRequest(String encryptedContent, String encryptionNonce) {
        this.encryptedContent = encryptedContent;
        this.encryptionNonce = encryptionNonce;
    }

    public MessageCreateRequest(String encryptedContent, String encryptionNonce, String senderPublicKey) {
        this.encryptedContent = encryptedContent;
        this.encryptionNonce = encryptionNonce;
        this.senderPublicKey = senderPublicKey;
    }

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getEncryptionNonce() { return encryptionNonce; }
    public void setEncryptionNonce(String encryptionNonce) { this.encryptionNonce = encryptionNonce; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }
    
    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }
}
