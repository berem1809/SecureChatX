package com.chatapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UserEncryptionKey entity for storing user's public ECDH key
 * 
 * DESIGN:
 * - Stores ONLY the public key (Base64 encoded)
 * - Private keys are NEVER stored on server
 * - One-to-one relationship with User
 * - Updated when user rotates keys
 * 
 * KEY FLOW:
 * 1. User generates ECDH keypair locally
 * 2. User uploads ONLY public key to this table
 * 3. Server stores public key associated with user
 * 4. Other users retrieve this public key to derive shared secrets
 * 5. Private key remains on user's device only
 */
@Entity
@Table(name = "user_encryption_keys", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class UserEncryptionKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User associated with this encryption key
     * One-to-one relationship
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * ECDH Public Key (Base64 encoded)
     * This is shared with other users for key exchange
     * NOT sensitive - can be transmitted over HTTPS
     */
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    /**
     * Algorithm version for future-proofing
     * Currently: "ECDH-XChaCha20-Poly1305"
     */
    @Column(name = "algorithm", nullable = false)
    private String algorithm = "ECDH-XChaCha20-Poly1305";

    /**
     * Timestamp when key was uploaded/last rotated
     */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    /**
     * Timestamp when key will expire (optional)
     * For key rotation policies
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public UserEncryptionKey() {}

    public UserEncryptionKey(User user, String publicKey) {
        this.user = user;
        this.publicKey = publicKey;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /**
     * Check if key is still valid (not expired)
     */
    public boolean isValid() {
        if (expiresAt == null) {
            return true; // No expiration set
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }
}
