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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing messages in conversations.
 */
public interface MessageService {

    /**
     * Sends a message in a conversation.
     */
    MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request);

    /**
     * Gets all messages in a conversation.
     */
    List<MessageResponse> getMessages(Long conversationId, Long userId);
}

@Service
class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationValidator conversationValidator;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              UserRepository userRepository,
                              ConversationValidator conversationValidator) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationValidator = conversationValidator;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, MessageCreateRequest request) {
        logger.info("User {} sending message to conversation {}", senderId, conversationId);

        // Validate user is part of the conversation
        Conversation conversation = conversationValidator.validateAndGetConversation(conversationId, senderId);

        // Get sender
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Create and save message
        Message message = new Message(conversation, sender, request.getContent());
        Message savedMessage = messageRepository.save(message);

        // Update conversation's last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        logger.info("Message {} sent to conversation {}", savedMessage.getId(), conversationId);
        return MessageResponse.fromEntity(savedMessage);
    }

    @Override
    public List<MessageResponse> getMessages(Long conversationId, Long userId) {
        logger.debug("Getting messages for conversation {} by user {}", conversationId, userId);

        // Validate user is part of the conversation
        conversationValidator.validateAndGetConversation(conversationId, userId);

        // Get messages
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
            .stream()
            .map(MessageResponse::fromEntity)
            .collect(Collectors.toList());
    }
}
