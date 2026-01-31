package com.chatapp.service;

import com.chatapp.dto.MessageCreateRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.exception.ConversationNotFoundException;
import com.chatapp.exception.GroupAccessDeniedException;
import com.chatapp.exception.GroupNotFoundException;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.model.Conversation;
import com.chatapp.model.Group;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.GroupMemberRepository;
import com.chatapp.repository.GroupRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.ConversationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing messages in conversations with E2EE support.
 * 
 * Supports both:
 * - Direct (1-to-1) conversations: Uses ECDH key exchange
 * - Group conversations: Uses symmetric group key
 * 
 * DESIGN PRINCIPLES:
 * - Receives encrypted messages from client
 * - Stores encrypted content as-is (NEVER decrypts)
 * - Returns encrypted messages to requesting users
 * - Server remains blind to message content
 * - All encryption/decryption happens client-side
 */
public interface MessageService {

    /**
     * Sends an encrypted message in a conversation (direct or group).
     * @param conversationId ID of the conversation (or group ID for groups)
     * @param senderId ID of the sender
     * @param request Encrypted message (encryptedContent, encryptionNonce)
     * @return Response with encrypted message data
     */
    MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request);

    /**
     * Gets all messages in a conversation (direct or group).
     * Returns encrypted messages that recipients must decrypt.
     * 
     * @param conversationId ID of the conversation (or group ID for groups)
     * @param userId ID of the requesting user
     * @param isGroup Optional flag to explicitly indicate this is a group (helps when IDs collide)
     * @return List of encrypted messages
     */
    List<MessageResponse> getMessages(Long conversationId, Long userId, Boolean isGroup);
}

@Slf4j
@Service
class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ConversationValidator conversationValidator;
    private final MessageEncryptionService encryptionService;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              GroupRepository groupRepository,
                              GroupMemberRepository groupMemberRepository,
                              UserRepository userRepository,
                              ConversationValidator conversationValidator,
                              MessageEncryptionService encryptionService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.conversationValidator = conversationValidator;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request) {
        log.info("📨 User {} sending encrypted message to conversation/group {}", senderId, conversationId);

        // Get sender
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new UserNotFoundException(senderId));

        // Validate encrypted message format
        encryptionService.validateEncryptedMessage(request);
        encryptionService.validateMessageSize(request);

        // Prepare encrypted message
        Message message = encryptionService.prepareEncryptedMessage(request, senderId);
        message.setSender(sender);

        // Use isGroupMessage flag from request to determine routing (avoids ID collision issues)
        Boolean isGroupMessage = request.getIsGroupMessage();
        
        if (Boolean.TRUE.equals(isGroupMessage)) {
            // EXPLICITLY a GROUP message - route to group
            log.info("📤 Routing to GROUP {} (explicit isGroupMessage=true)", conversationId);
            
            Optional<Group> groupOpt = groupRepository.findById(conversationId);
            if (groupOpt.isEmpty()) {
                throw new GroupNotFoundException(conversationId);
            }
            
            Group group = groupOpt.get();
            
            // Validate user is a member of the group
            if (!groupMemberRepository.existsByGroupIdAndUserId(conversationId, senderId)) {
                throw new GroupAccessDeniedException(conversationId, senderId);
            }
            
            message.setGroup(group);
            message.setConversation(null);
            
            // Update group's last message time
            group.setLastMessageAt(LocalDateTime.now());
            groupRepository.save(group);
        } else {
            // Try to find as direct conversation first (default behavior for backward compatibility)
            Optional<Conversation> directConv = conversationRepository.findByIdAndUserId(conversationId, senderId);
            
            if (directConv.isPresent()) {
                // DIRECT CONVERSATION
                log.info("📤 Sending to DIRECT conversation {}", conversationId);
                Conversation conversation = directConv.get();
                message.setConversation(conversation);
                message.setGroup(null);
                
                // Update conversation's last message time
                conversation.setLastMessageAt(LocalDateTime.now());
                conversationRepository.save(conversation);
            } else {
                // Fallback: Try as GROUP conversation (for backward compatibility when isGroupMessage is not set)
                Optional<Group> groupOpt = groupRepository.findById(conversationId);
                
                if (groupOpt.isPresent()) {
                    Group group = groupOpt.get();
                    
                    // Validate user is a member of the group
                    if (!groupMemberRepository.existsByGroupIdAndUserId(conversationId, senderId)) {
                        throw new GroupAccessDeniedException(conversationId, senderId);
                    }
                    
                    log.info("📤 Sending to GROUP conversation {} (fallback)", conversationId);
                    message.setGroup(group);
                    message.setConversation(null);
                    
                    // Update group's last message time
                    group.setLastMessageAt(LocalDateTime.now());
                    groupRepository.save(group);
                } else {
                    throw new ConversationNotFoundException(conversationId);
                }
            }
        }

        // Save encrypted message to database
        Message savedMessage = messageRepository.save(message);

        // Audit log (WITHOUT logging plaintext)
        encryptionService.auditMessageOperation(savedMessage, senderId, "SEND");

        log.info("✅ Encrypted message {} sent", savedMessage.getId());

        // Return encrypted response (no decryption on server)
        return encryptionService.createEncryptedResponse(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long userId, Boolean isGroup) {
        log.debug("📥 User {} retrieving encrypted messages from conversation/group {} (isGroup={})", userId, conversationId, isGroup);

        List<Message> messages;

        // Use isGroup flag to determine routing (avoids ID collision issues)
        if (Boolean.TRUE.equals(isGroup)) {
            // EXPLICITLY a GROUP - fetch from group
            log.debug("📥 Fetching from GROUP {} (explicit isGroup=true)", conversationId);
            
            Optional<Group> groupOpt = groupRepository.findById(conversationId);
            if (groupOpt.isEmpty()) {
                throw new GroupNotFoundException(conversationId);
            }
            
            // Validate user is a member of the group
            if (!groupMemberRepository.existsByGroupIdAndUserId(conversationId, userId)) {
                throw new GroupAccessDeniedException(conversationId, userId);
            }
            
            messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(conversationId);
        } else {
            // Try to find as direct conversation first (default behavior)
            Optional<Conversation> directConv = conversationRepository.findByIdAndUserId(conversationId, userId);
            
            if (directConv.isPresent()) {
                // DIRECT CONVERSATION
                log.debug("📥 Fetching from DIRECT conversation {}", conversationId);
                messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            } else {
                // Fallback: Try as GROUP conversation (for backward compatibility)
                Optional<Group> groupOpt = groupRepository.findById(conversationId);
                
                if (groupOpt.isPresent()) {
                    // Validate user is a member of the group
                    if (!groupMemberRepository.existsByGroupIdAndUserId(conversationId, userId)) {
                        throw new GroupAccessDeniedException(conversationId, userId);
                    }
                    
                    log.debug("📥 Fetching from GROUP conversation {} (fallback)", conversationId);
                    messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(conversationId);
                } else {
                    throw new ConversationNotFoundException(conversationId);
                }
            }
        }

        // Convert to encrypted responses (NO DECRYPTION)
        List<MessageResponse> responses = messages.stream()
            .map(msg -> {
                // Log audit (WITHOUT plaintext)
                encryptionService.auditMessageOperation(msg, userId, "RECEIVE");
                
                // Return encrypted data only
                return encryptionService.createEncryptedResponse(msg);
            })
            .collect(Collectors.toList());

        log.debug("✅ Retrieved {} encrypted messages from conversation/group {}",
                responses.size(), conversationId);

        return responses;
    }
}
