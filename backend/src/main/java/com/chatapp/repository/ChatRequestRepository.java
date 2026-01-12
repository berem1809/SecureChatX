package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.ChatRequest;
import com.chatapp.model.ChatRequestStatus;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CHAT REQUEST REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for chat requests.
 * 
 * KEY QUERIES:
 * ------------
 * - Find requests sent by a user
 * - Find requests received by a user
 * - Check if request exists between two users
 * - Find pending requests for a user
 */
public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {

    /**
     * Finds all chat requests sent by a specific user.
     * 
     * @param sender The user who sent the requests
     * @return List of chat requests
     */
    List<ChatRequest> findBySender(User sender);

    /**
     * Finds all chat requests received by a specific user.
     * 
     * @param receiver The user who received the requests
     * @return List of chat requests
     */
    List<ChatRequest> findByReceiver(User receiver);

    /**
     * Finds all chat requests sent by a user with a specific status.
     * 
     * @param senderId The sender's user ID
     * @param status The request status to filter by
     * @return List of matching chat requests
     */
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.sender.id = :senderId AND cr.status = :status")
    List<ChatRequest> findBySenderIdAndStatus(@Param("senderId") Long senderId, 
                                               @Param("status") ChatRequestStatus status);

    /**
     * Finds all chat requests received by a user with a specific status.
     * 
     * @param receiverId The receiver's user ID
     * @param status The request status to filter by
     * @return List of matching chat requests
     */
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.receiver.id = :receiverId AND cr.status = :status")
    List<ChatRequest> findByReceiverIdAndStatus(@Param("receiverId") Long receiverId, 
                                                 @Param("status") ChatRequestStatus status);

    /**
     * Checks if a chat request already exists between two users (in either direction).
     * This prevents duplicate requests.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return true if a request exists between these users
     */
    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END FROM ChatRequest cr " +
           "WHERE (cr.sender.id = :user1Id AND cr.receiver.id = :user2Id) " +
           "OR (cr.sender.id = :user2Id AND cr.receiver.id = :user1Id)")
    boolean existsBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * Finds a chat request between two specific users (in either direction).
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return Optional containing the chat request if found
     */
    @Query("SELECT cr FROM ChatRequest cr " +
           "WHERE (cr.sender.id = :user1Id AND cr.receiver.id = :user2Id) " +
           "OR (cr.sender.id = :user2Id AND cr.receiver.id = :user1Id)")
    Optional<ChatRequest> findBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * Finds a specific chat request by sender and receiver.
     * 
     * @param senderId Sender's user ID
     * @param receiverId Receiver's user ID
     * @return Optional containing the chat request if found
     */
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.sender.id = :senderId AND cr.receiver.id = :receiverId")
    Optional<ChatRequest> findBySenderIdAndReceiverId(@Param("senderId") Long senderId, 
                                                       @Param("receiverId") Long receiverId);

    /**
     * Finds all chat requests sent by a user, ordered by creation date (newest first).
     * 
     * @param senderId The sender's user ID
     * @return List of chat requests ordered by created_at DESC
     */
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.sender.id = :senderId ORDER BY cr.createdAt DESC")
    List<ChatRequest> findBySenderIdOrderByCreatedAtDesc(@Param("senderId") Long senderId);

    /**
     * Finds all chat requests received by a user, ordered by creation date (newest first).
     * 
     * @param receiverId The receiver's user ID
     * @return List of chat requests ordered by created_at DESC
     */
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.receiver.id = :receiverId ORDER BY cr.createdAt DESC")
    List<ChatRequest> findByReceiverIdOrderByCreatedAtDesc(@Param("receiverId") Long receiverId);

    /**
     * Counts pending chat requests received by a user.
     * Useful for notification badges.
     * 
     * @param receiverId The receiver's user ID
     * @return Count of pending requests
     */
    @Query("SELECT COUNT(cr) FROM ChatRequest cr WHERE cr.receiver.id = :receiverId AND cr.status = 'PENDING'")
    long countPendingByReceiverId(@Param("receiverId") Long receiverId);
}
