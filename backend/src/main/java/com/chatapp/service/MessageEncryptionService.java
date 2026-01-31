package com.chatapp.service;

import com.chatapp.dto.MessageCreateRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.model.Message;
import com.chatapp.model.UserEncryptionKey;
import com.chatapp.repository.UserEncryptionKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * MessageEncryptionService - Handles encrypted message operations
 * 
 * RESPONSIBILITY:
 * - Validates encrypted message format
 * - Stores encrypted messages securely
 * - Retrieves and returns encrypted messages to clients
 * - NEVER decrypts messages (server is blind)
 * 
 * KEY DESIGN PRINCIPLES:
 * 1. Server NEVER sees plaintext
 * 2. Server validates message structure but not content
 * 3. Only metadata (sender, timestamp) is plaintext
 * 4. Message authenticity verified by client using Poly1305 MAC
 * 
 * ENCRYPTION FLOW:
 * Frontend (Client-side):
 *   plaintext → XSalsa20-Poly1305(plaintext, sharedSecret) → ciphertext
 *   ciphertext + nonce + senderPublicKey → sent to server
 * 
 * Backend (Server-side):
 *   receive(ciphertext, nonce, senderPublicKey) → store as-is → NO DECRYPTION
 * 
 * Frontend (Client-side - Recipient):
 *   receive(ciphertext, nonce, senderPublicKey)
 *   derive sharedSecret(myPrivateKey, senderPublicKey)
 *   plaintext ← XSalsa20-Poly1305.open(ciphertext, nonce, sharedSecret)
 */
@Slf4j
@Service
public class MessageEncryptionService {

    private final UserEncryptionKeyRepository encryptionKeyRepository;

    public MessageEncryptionService(UserEncryptionKeyRepository encryptionKeyRepository) {
        this.encryptionKeyRepository = encryptionKeyRepository;
    }

    /**
     * Validate and prepare encrypted message for storage
     * 
     * @param request MessageCreateRequest containing encrypted content
     * @param sender User sending the message
     * @return Message entity with encrypted fields populated
     */
    public Message prepareEncryptedMessage(MessageCreateRequest request, Long senderId) {
        log.debug("📨 Preparing encrypted message from user {}", senderId);

        // Validate encrypted content format
        validateEncryptedMessage(request);

        // Create message entity
        Message message = new Message();
        
        // For encrypted messages, set content to "[encrypted]" placeholder
        // since the actual content is in encryptedContent field
        message.setContent("[encrypted]");
        message.setEncryptedContent(request.getEncryptedContent());
        message.setEncryptionNonce(request.getEncryptionNonce());
        
        // Use the sender's public key from the request (sent by client)
        // This ensures we use the exact public key that was used for encryption
        message.setSenderPublicKey(request.getSenderPublicKey());
        message.setIsEncrypted(true);

        log.debug("✅ Encrypted message prepared (size: {} bytes)", 
                request.getEncryptedContent().length());
        
        return message;
    }

    /**
     * Validate encrypted message structure
     * 
     * Checks:
     * - encryptedContent is not null/empty
     * - encryptedContent is valid Base64
     * - encryptionNonce is not null/empty
     * - encryptionNonce is valid Base64
     * 
     * @param request MessageCreateRequest to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateEncryptedMessage(MessageCreateRequest request) {
        log.debug("🔍 Validating encrypted message structure");

        // Validate encrypted content
        if (request.getEncryptedContent() == null || request.getEncryptedContent().isEmpty()) {
            log.warn("❌ Encrypted content is null or empty");
            throw new IllegalArgumentException("Encrypted content cannot be empty");
        }

        // Validate it's valid Base64
        try {
            Base64.getDecoder().decode(request.getEncryptedContent());
            log.debug("✅ Encrypted content is valid Base64");
        } catch (IllegalArgumentException e) {
            log.warn("❌ Encrypted content is not valid Base64: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 encoding for encrypted content");
        }

        // Validate encryption nonce
        if (request.getEncryptionNonce() == null || request.getEncryptionNonce().isEmpty()) {
            log.warn("❌ Encryption nonce is null or empty");
            throw new IllegalArgumentException("Encryption nonce cannot be empty");
        }

        // Validate nonce is valid Base64
        byte[] nonceBytes = null;
        try {
            nonceBytes = Base64.getDecoder().decode(request.getEncryptionNonce());
            log.debug("✅ Encryption nonce is valid Base64, length: {} bytes", nonceBytes.length);
        } catch (IllegalArgumentException e) {
            log.warn("❌ Encryption nonce is not valid Base64: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 encoding for encryption nonce");
        }

        // XSalsa20-Poly1305 uses 24-byte nonce - allow some flexibility during debugging
        if (nonceBytes.length != 24) {
            log.warn("⚠️ Nonce length is {} bytes, expected 24 bytes. This may cause decryption issues.", nonceBytes.length);
            // For now, we'll allow it but warn. In production, make this stricter
        }

        log.debug("✅ Encrypted message structure is valid");
    }

    /**
     * Validate message size doesn't exceed limits
     * 
     * Limits:
     * - Plaintext: 4MB (before encryption, estimated)
     * - Ciphertext: 4MB (after encryption, slightly larger)
     * 
     * @param request MessageCreateRequest
     * @throws IllegalArgumentException if message is too large
     */
    public void validateMessageSize(MessageCreateRequest request) {
        final long MAX_ENCRYPTED_SIZE = 4 * 1024 * 1024; // 4MB
        
        long encryptedSize = request.getEncryptedContent().getBytes().length;
        
        if (encryptedSize > MAX_ENCRYPTED_SIZE) {
            log.warn("❌ Message size {} bytes exceeds limit of {}", 
                    encryptedSize, MAX_ENCRYPTED_SIZE);
            throw new IllegalArgumentException(
                    "Message is too large. Maximum size: " + MAX_ENCRYPTED_SIZE + " bytes");
        }
        
        log.debug("✅ Message size is valid ({} bytes)", encryptedSize);
    }

    /**
     * Log audit information about encrypted message
     * (WITHOUT decrypting or logging plaintext)
     * 
     * @param message Message entity (encrypted)
     * @param userId User performing the action
     * @param action "SEND" or "RECEIVE"
     */
    public void auditMessageOperation(Message message, Long userId, String action) {
        // Get conversation or group ID based on message type
        Long contextId = null;
        String contextType = "unknown";

        if (message.getConversation() != null) {
            contextId = message.getConversation().getId();
            contextType = "conversation";
        } else if (message.getGroup() != null) {
            contextId = message.getGroup().getId();
            contextType = "group";
        }
        
        log.info("🔐 [AUDIT] {} encrypted message {} by user {} in {} {}",
                action,
                message.getId() != null ? message.getId() : "NEW",
                userId,
                contextType,
                contextId);
        
        if (message.getSender() != null) {
            log.debug("  - Sender: {}", message.getSender().getId());
        }
        log.debug("  - Encrypted size: {} bytes",
                message.getEncryptedContent() != null ? message.getEncryptedContent().length() : 0);
        log.debug("  - Algorithm: XSalsa20-Poly1305 / NaCl SecretBox");
        log.debug("  - Timestamp: {}",
                message.getCreatedAt());
        
        // ⚠️ NEVER log plaintext or decrypted content
        // ⚠️ NEVER log private keys or shared secrets
    }

    /**
     * Convert encrypted Message entity to MessageResponse
     * Returns only encrypted data (no decryption on server)
     * 
     * @param message Message entity with encrypted content
     * @return MessageResponse with encrypted fields
     */
    public MessageResponse createEncryptedResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getEmail());
        response.setSenderEmail(message.getSender().getEmail());
        response.setSenderDisplayName(message.getSender().getDisplayName());
        
        // Set conversation/group identification fields
        if (message.getGroup() != null) {
            // GROUP MESSAGE
            response.setIsGroup(true);
            response.setGroupId(message.getGroup().getId());
            response.setConversationId(message.getGroup().getId()); // For backward compatibility
        } else if (message.getConversation() != null) {
            // DIRECT MESSAGE
            response.setIsGroup(false);
            response.setGroupId(null);
            response.setConversationId(message.getConversation().getId());
        }
        
        // Return ENCRYPTED content (not plaintext)
        response.setEncryptedContent(message.getEncryptedContent());
        response.setEncryptionNonce(message.getEncryptionNonce());
        response.setSenderPublicKey(message.getSenderPublicKey());
        response.setIsEncrypted(message.getIsEncrypted());
        
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());
        
        return response;
    }

    /**
     * Verify sender's public key matches the one in message
     * 
     * This ensures:
     * - Message wasn't tampered with
     * - Public key in message is current/valid
     * 
     * @param message Message to verify
     * @return true if public key matches current sender key
     */
    public boolean verifySenderPublicKey(Message message) {
        UserEncryptionKey senderKey = encryptionKeyRepository.findByUserId(message.getSender().getId())
                .orElse(null);
        
        if (senderKey == null) {
            log.warn("❌ Sender's encryption key not found");
            return false;
        }
        
        boolean matches = senderKey.getPublicKey().equals(message.getSenderPublicKey());
        
        if (!matches) {
            log.warn("⚠️ Sender's public key in message doesn't match current key");
            log.debug("  - Message key: {}", message.getSenderPublicKey().substring(0, 20) + "...");
            log.debug("  - Current key: {}", senderKey.getPublicKey().substring(0, 20) + "...");
        }
        
        return matches;
    }

    /**
     * Rotate sender's encryption key
     * Used when user changes their ECDH keypair
     * 
     * @param userId User rotating their key
     * @param newPublicKey New X25519 public key (Base64)
     */
    public void rotateEncryptionKey(Long userId, String newPublicKey) {
        log.info("🔄 Rotating encryption key for user {}", userId);
        
        UserEncryptionKey key = encryptionKeyRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User encryption key not found"));
        
        // Log old key (first 20 chars) for audit
        String oldKeyPreview = key.getPublicKey().substring(0, Math.min(20, key.getPublicKey().length()));
        String newKeyPreview = newPublicKey.substring(0, Math.min(20, newPublicKey.length()));
        
        log.info("📝 Key rotation: {} → {}", oldKeyPreview + "...", newKeyPreview + "...");
        
        key.setPublicKey(newPublicKey);
        encryptionKeyRepository.save(key);
        
        log.info("✅ Encryption key rotated successfully for user {}", userId);
    }
}
