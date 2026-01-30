package com.chatapp.repository;

import com.chatapp.model.User;
import com.chatapp.model.UserEncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserEncryptionKey entity
 * Handles database operations for ECDH public keys
 */
@Repository
public interface UserEncryptionKeyRepository extends JpaRepository<UserEncryptionKey, Long> {

    /**
     * Find encryption key by user
     */
    Optional<UserEncryptionKey> findByUser(User user);

    /**
     * Find encryption key by user ID
     */
    Optional<UserEncryptionKey> findByUserId(Long userId);

    /**
     * Check if user has encryption key
     */
    boolean existsByUser(User user);
}
