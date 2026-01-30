package com.chatapp.service;

import com.chatapp.dto.PublicKeyResponse;
import com.chatapp.dto.PublicKeyUploadRequest;
import com.chatapp.model.User;
import com.chatapp.model.UserEncryptionKey;
import com.chatapp.repository.UserEncryptionKeyRepository;
import com.chatapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EncryptionService - Handles public key management for ECDH
 * 
 * RESPONSIBILITIES:
 * - Store user's public ECDH key
 * - Retrieve public keys for encryption/key exchange
 * - Support key rotation
 * 
 * SECURITY:
 * - ONLY public keys are stored (private keys stay on client)
 * - Keys are transmitted over HTTPS
 * - Server has no access to plaintext messages or private keys
 */
@Slf4j
@Service
@Transactional
public class EncryptionService {

    private final UserEncryptionKeyRepository keyRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public EncryptionService(
        UserEncryptionKeyRepository keyRepository,
        UserRepository userRepository,
        EntityManager entityManager
    ) {
        this.keyRepository = keyRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    /**
     * Upload or update user's public key
     * Called when user initializes or rotates encryption keys
     */
    public PublicKeyResponse uploadPublicKey(Long userId, PublicKeyUploadRequest request) {
        log.info("Uploading public key for user {}", userId);

        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("User not found with ID: {}", userId);
                return new IllegalArgumentException("User not found");
            });
        
        log.debug("Found user: {} ({})", user.getEmail(), user.getId());

        // Check if key already exists
        UserEncryptionKey key = keyRepository.findByUser(user)
            .orElse(null);
        
        if (key == null) {
            log.info("Creating new encryption key for user {}", userId);
            key = new UserEncryptionKey(user, request.getPublicKey());
        } else {
            log.info("Rotating encryption key for user {}", userId);
            key.setPublicKey(request.getPublicKey());
        }

        key.setUploadedAt(java.time.LocalDateTime.now());
        
        log.debug("Saving encryption key: publicKey length = {}, user = {}", 
            request.getPublicKey().length(), user.getEmail());

        UserEncryptionKey saved = keyRepository.save(key);
        log.info("✅ Public key saved successfully for user {} with ID {}", userId, saved.getId());
        
        // IMPORTANT: Flush to ensure the insert/update is written to the database immediately
        // Without this, the data stays in Hibernate's session cache and might not be visible to subsequent queries
        entityManager.flush();
        log.debug("Flushed entity manager - data written to database");
        
        // Verify the save was successful by checking if we can retrieve it
        boolean verifyExists = keyRepository.findByUserId(userId).isPresent();
        log.info("Verification: Public key exists in DB = {}", verifyExists);
        
        if (!verifyExists) {
            log.error("❌ CRITICAL: Public key save was not persisted to database despite flush!");
        }

        return PublicKeyResponse.fromEntity(saved);
    }

    /**
     * Retrieve public key for a user by ID
     * Used by other users to derive shared secrets
     */
    public PublicKeyResponse getPublicKey(Long userId) {
        log.debug("Fetching public key for user {}", userId);

        var keyOpt = keyRepository.findByUserId(userId);
        
        if (keyOpt.isEmpty()) {
            log.warn("❌ Public key not found for user ID: {}", userId);
            log.warn("Available keys in DB: {}", keyRepository.count());
            throw new IllegalArgumentException(
                "Public key not found for user. User may not have encryption initialized."
            );
        }

        UserEncryptionKey key = keyOpt.get();
        log.debug("Found public key for user {}: algorithm={}, uploadedAt={}", 
            userId, key.getAlgorithm(), key.getUploadedAt());

        if (!key.isValid()) {
            log.warn("Public key has expired for user {}", userId);
            throw new IllegalArgumentException("Public key has expired");
        }

        log.debug("✅ Returning public key for user {}", userId);
        return PublicKeyResponse.fromEntity(key);
    }

    /**
     * Retrieve public keys for multiple users (batch operation)
     */
    public List<PublicKeyResponse> getPublicKeys(List<Long> userIds) {
        log.debug("Fetching public keys for {} users", userIds.size());

        return userIds.stream()
            .map(this::getPublicKey)
            .collect(Collectors.toList());
    }

    /**
     * Check if user has encryption key set up
     */
    public boolean hasPublicKey(Long userId) {
        return keyRepository.findByUserId(userId).isPresent();
    }

    /**
     * Delete user's encryption key (on account deletion)
     */
    public void deletePublicKey(Long userId) {
        log.info("Deleting public key for user {}", userId);
        keyRepository.findByUserId(userId).ifPresent(keyRepository::delete);
    }

    /**
     * Get or create default encryption key for user
     * (If user doesn't have one, this can be called after login to auto-initialize)
     */
    public PublicKeyResponse getOrInitialize(Long userId, String defaultPublicKey) {
        if (hasPublicKey(userId)) {
            return getPublicKey(userId);
        }

        log.info("Auto-initializing encryption for user {}", userId);
        return uploadPublicKey(userId, new PublicKeyUploadRequest(defaultPublicKey));
    }
}
