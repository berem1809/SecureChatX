package com.chatapp.service;

import com.chatapp.dto.MessageCreateRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.model.Conversation;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.ConversationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing messages in conversations with E2EE support.
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
     * Sends an encrypted message in a conversation.
     * @param conversationId ID of the conversation
     * @param senderId ID of the sender
     * @param request Encrypted message (encryptedContent, encryptionNonce)
     * @return Response with encrypted message data
     */
    MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request);

    /**
     * Gets all messages in a conversation.
     * Returns encrypted messages that recipients must decrypt.
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the requesting user
     * @return List of encrypted messages
     */
    List<MessageResponse> getMessages(Long conversationId, Long userId);
}

@Slf4j
@Service
class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationValidator conversationValidator;
    private final MessageEncryptionService encryptionService;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              UserRepository userRepository,
                              ConversationValidator conversationValidator,
                              MessageEncryptionService encryptionService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationValidator = conversationValidator;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request) {
        log.info("📨 User {} sending encrypted message to conversation {}", senderId, conversationId);

        // Validate user is part of the conversation
        Conversation conversation = conversationValidator.validateAndGetConversation(conversationId, senderId);

        // Get sender
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate encrypted message format
        encryptionService.validateEncryptedMessage(request);
        encryptionService.validateMessageSize(request);

        // Prepare encrypted message (sets encryptedContent, nonce, senderPublicKey)
        Message message = encryptionService.prepareEncryptedMessage(request, senderId);
        message.setConversation(conversation);
        message.setSender(sender);

        // Save encrypted message to database
        Message savedMessage = messageRepository.save(message);

        // Audit log (WITHOUT logging plaintext)
        encryptionService.auditMessageOperation(savedMessage, senderId, "SEND");

        // Update conversation's last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("✅ Encrypted message {} sent to conversation {}", savedMessage.getId(), conversationId);

        // Return encrypted response (no decryption on server)
        return encryptionService.createEncryptedResponse(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long userId) {
        log.debug("📥 User {} retrieving encrypted messages from conversation {}", userId, conversationId);

        // Validate user is part of the conversation
        conversationValidator.validateAndGetConversation(conversationId, userId);

        // Get encrypted messages
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Convert to encrypted responses (NO DECRYPTION)
        List<MessageResponse> responses = messages.stream()
            .map(msg -> {
                // Log audit (WITHOUT plaintext)
                encryptionService.auditMessageOperation(msg, userId, "RECEIVE");
                
                // Return encrypted data only
                return encryptionService.createEncryptedResponse(msg);
            })
            .collect(Collectors.toList());

        log.debug("✅ Retrieved {} encrypted messages from conversation {}",
                responses.size(), conversationId);

        return responses;
    }
}
