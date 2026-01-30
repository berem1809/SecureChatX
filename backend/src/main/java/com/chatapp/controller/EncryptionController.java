package com.chatapp.controller;

import com.chatapp.dto.PublicKeyResponse;
import com.chatapp.dto.PublicKeyUploadRequest;
import com.chatapp.dto.ErrorResponse;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import com.chatapp.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EncryptionController - Handles ECDH public key exchange endpoints
 * 
 * ENDPOINTS:
 * - POST /api/crypto/keys/public - Upload user's public key
 * - GET /api/crypto/keys/public/{userId} - Retrieve another user's public key
 * - POST /api/crypto/keys/public/batch - Batch retrieve public keys
 * 
 * SECURITY:
 * - Requires authentication for uploads
 * - Public key retrieval is public (keys are not sensitive)
 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/keys")
public class EncryptionController {

    private final EncryptionService encryptionService;
    private final UserRepository userRepository;

    public EncryptionController(
        EncryptionService encryptionService,
        UserRepository userRepository
    ) {
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
    }

    /**
     * Upload user's public ECDH key
     * Called when user initializes or rotates encryption
     * 
     * @param request PublicKeyUploadRequest containing Base64 encoded public key
     * @return PublicKeyResponse with confirmation
     */
    @PostMapping("/public")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> uploadPublicKey(
        @RequestBody PublicKeyUploadRequest request,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("POST /api/crypto/keys/public - User {} uploading public key", userId);

        if (request.getPublicKey() == null || request.getPublicKey().isEmpty()) {
            log.warn("Public key is empty or null");
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Invalid request", "Public key cannot be empty"));
        }

        try {
            PublicKeyResponse response = encryptionService.uploadPublicKey(userId, request);
            log.info("✅ Public key uploaded successfully for user {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid argument when uploading public key for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "User not found", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Failed to upload public key for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to upload public key", e.getMessage()));
        }
    }

    /**
     * Retrieve another user's public key for ECDH key exchange
     * This is NOT sensitive - public keys are meant to be shared
     * 
     * @param userId The user whose public key to retrieve
     * @return PublicKeyResponse
     */
    @GetMapping("/public/{userId}")
    public ResponseEntity<?> getPublicKey(
        @PathVariable Long userId
    ) {
        log.debug("GET /api/crypto/keys/public/{} - Retrieving public key", userId);

        try {
            PublicKeyResponse response = encryptionService.getPublicKey(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Public key not found for user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ Failed to retrieve public key for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to retrieve public key", e.getMessage()));
        }
    }

    /**
     * Batch retrieve public keys for multiple users
     * More efficient than individual requests
     * 
     * @param userIds List of user IDs
     * @return List of PublicKeyResponse
     */
    @PostMapping("/public/batch")
    public ResponseEntity<?> getPublicKeys(
        @RequestBody List<Long> userIds
    ) {
        log.debug("POST /api/crypto/keys/public/batch - Retrieving {} public keys", userIds.size());

        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Invalid request", "User IDs list cannot be empty"));
        }

        try {
            List<PublicKeyResponse> responses = encryptionService.getPublicKeys(userIds);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("❌ Failed to retrieve batch public keys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to retrieve public keys", e.getMessage()));
        }
    }

    /**
     * Check if user has encryption enabled
     */
    @GetMapping("/public/{userId}/exists")
    public ResponseEntity<Boolean> hasPublicKey(
        @PathVariable Long userId
    ) {
        log.debug("GET /api/crypto/keys/public/{}/exists - Checking if public key exists", userId);
        return ResponseEntity.ok(encryptionService.hasPublicKey(userId));
    }

    /**
     * Extract user ID from Spring Security Authentication object
     * Gets email from authentication, then looks up user in database
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }
}
