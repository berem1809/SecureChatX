package com.chatapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_conversation", columnList = "conversation_id"),
    @Index(name = "idx_message_group", columnList = "group_id"),
    @Index(name = "idx_message_sender", columnList = "sender_id"),
    @Index(name = "idx_message_created", columnList = "created_at")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * For direct (1-to-1) conversations
     * NULL for group messages
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = true)
    private Conversation conversation;

    /**
     * For group conversations
     * NULL for direct messages
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Plaintext content (only for unencrypted messages)
     * NULL for end-to-end encrypted messages
     */
    @Column(nullable = true, columnDefinition = "TEXT")  // ✅ CHANGED: nullable = true
    private String content;

    /**
     * Encrypted message content (XChaCha20-Poly1305 ciphertext)
     * Stored in Base64 format
     */
    @Column(name = "encrypted_content", columnDefinition = "TEXT")
    private String encryptedContent;

    /**
     * Nonce used for encryption (Base64 encoded)
     */
    @Column(name = "encryption_nonce")
    private String encryptionNonce;

    /**
     * Sender's public key at time of message (Base64 encoded)
     * Used for decryption verification
     */
    @Column(name = "sender_public_key", columnDefinition = "TEXT")
    private String senderPublicKey;

    /**
     * Flag indicating if message content is encrypted
     */
    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Message() {}

    public Message(Conversation conversation, User sender, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getEncryptionNonce() { return encryptionNonce; }
    public void setEncryptionNonce(String encryptionNonce) { this.encryptionNonce = encryptionNonce; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }
}