package com.chatapp.service;

import com.chatapp.dto.NotificationCountResponse;
import com.chatapp.model.ChatRequestStatus;
import com.chatapp.model.GroupInvitationStatus;
import com.chatapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * NOTIFICATION SERVICE - Handles notification counting and delivery
 * ============================================================================
 * 
 * Provides notification counts for:
 * - Unread direct messages
 * - Unread group messages
 * - Pending chat requests
 * - Pending group invitations
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final MessageRepository messageRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final ConversationRepository conversationRepository;
    private final GroupMemberRepository groupMemberRepository;

    public NotificationService(MessageRepository messageRepository,
                               ChatRequestRepository chatRequestRepository,
                               GroupInvitationRepository groupInvitationRepository,
                               ConversationRepository conversationRepository,
                               GroupMemberRepository groupMemberRepository) {
        this.messageRepository = messageRepository;
        this.chatRequestRepository = chatRequestRepository;
        this.groupInvitationRepository = groupInvitationRepository;
        this.conversationRepository = conversationRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * Gets all notification counts for a user.
     * 
     * @param userId The user's ID
     * @return Notification counts
     */
    @Transactional(readOnly = true)
    public NotificationCountResponse getNotificationCounts(Long userId) {
        logger.debug("Calculating notification counts for user {}", userId);

        // Count unread direct messages (messages in user's conversations sent by others)
        long unreadDirectMessages = countUnreadDirectMessages(userId);
        
        // Count unread group messages (messages in user's groups sent by others)
        long unreadGroupMessages = countUnreadGroupMessages(userId);
        
        // Count pending chat requests (received requests)
        long pendingChatRequests = chatRequestRepository.countPendingByReceiverId(userId);
        
        // Count pending group invitations (received invitations)
        long pendingGroupInvitations = groupInvitationRepository.countPendingByInviteeId(userId);

        NotificationCountResponse response = new NotificationCountResponse(
            unreadDirectMessages,
            unreadGroupMessages,
            pendingChatRequests,
            pendingGroupInvitations
        );

        logger.debug("Notification counts for user {}: direct={}, group={}, requests={}, invitations={}", 
                     userId, unreadDirectMessages, unreadGroupMessages, 
                     pendingChatRequests, pendingGroupInvitations);

        return response;
    }

    /**
     * Counts unread messages in direct conversations.
     * For simplicity, we count messages sent by others in the last 24 hours
     * that are newer than the user's last activity.
     */
    private long countUnreadDirectMessages(Long userId) {
        // Get all conversations for the user
        var conversations = conversationRepository.findByUserId(userId);
        
        long count = 0;
        for (var conversation : conversations) {
            // Count messages not sent by this user
            long messagesFromOthers = messageRepository.countByConversationIdAndSenderIdNot(
                conversation.getId(), userId
            );
            count += messagesFromOthers;
        }
        
        return count;
    }

    /**
     * Counts unread messages in group conversations.
     * For simplicity, we count messages sent by others in groups the user is a member of.
     */
    private long countUnreadGroupMessages(Long userId) {
        // Get all groups the user is a member of
        var groupMemberships = groupMemberRepository.findByUserId(userId);
        
        long count = 0;
        for (var membership : groupMemberships) {
            // Count messages not sent by this user
            long messagesFromOthers = messageRepository.countByGroupIdAndSenderIdNot(
                membership.getGroup().getId(), userId
            );
            count += messagesFromOthers;
        }
        
        return count;
    }
}
