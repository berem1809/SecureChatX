package com.chatapp.dto;

import com.chatapp.model.UserEncryptionKey;
import java.time.LocalDateTime;

/**
 * DTO for public key response
 * Contains only non-sensitive public key information
 */
public class PublicKeyResponse {

    private Long userId;
    private String email;
    private String displayName;
    private String publicKey;
    private String algorithm;
    private LocalDateTime uploadedAt;

    public PublicKeyResponse() {}

    public static PublicKeyResponse fromEntity(UserEncryptionKey key) {
        PublicKeyResponse response = new PublicKeyResponse();
        response.setUserId(key.getUser().getId());
        response.setEmail(key.getUser().getEmail());
        response.setDisplayName(key.getUser().getDisplayName());
        response.setPublicKey(key.getPublicKey());
        response.setAlgorithm(key.getAlgorithm());
        response.setUploadedAt(key.getUploadedAt());
        return response;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
