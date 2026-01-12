package com.chatapp.controller;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.dto.UserSearchResponse;
import com.chatapp.model.Conversation;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ConversationController.
 * Tests all conversation REST endpoints.
 */
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private User otherUser;
    private ConversationResponse testConversationResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ConversationController(conversationService, userRepository))
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test users
        testUser = new User("test@example.com", "password", "Test User", "ACTIVE", List.of("ROLE_USER"));
        testUser.setId(1L);

        otherUser = new User("other@example.com", "password", "Other User", "ACTIVE", List.of("ROLE_USER"));
        otherUser.setId(2L);

        // Setup test conversation response using actual structure
        Conversation conversation = new Conversation(testUser, otherUser);
        conversation.setId(100L);
        testConversationResponse = ConversationResponse.fromEntityWithCurrentUser(conversation, 1L);
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            "test@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ========================================================================
    // GET CONVERSATIONS TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/conversations")
    class GetConversationsEndpoint {

        @Test
        @DisplayName("Gets all user conversations")
        void getUserConversations_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.getUserConversations(1L))
                .thenReturn(List.of(testConversationResponse));

            // Act & Assert
            mockMvc.perform(get("/api/conversations")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(100L));

            verify(conversationService).getUserConversations(1L);
        }

        @Test
        @DisplayName("Returns empty list when no conversations")
        void getUserConversations_Empty() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.getUserConversations(1L)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/conversations")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

            verify(conversationService).getUserConversations(1L);
        }
    }

    // ========================================================================
    // GET SPECIFIC CONVERSATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/conversations/{id}")
    class GetConversationEndpoint {

        @Test
        @DisplayName("Gets specific conversation")
        void getConversation_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.getConversation(100L, 1L)).thenReturn(testConversationResponse);

            // Act & Assert
            mockMvc.perform(get("/api/conversations/100")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.user1.id").value(1L))
                .andExpect(jsonPath("$.user2.id").value(2L));

            verify(conversationService).getConversation(100L, 1L);
        }
    }

    // ========================================================================
    // GET CONVERSATION WITH USER TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/conversations/with/{userId}")
    class GetConversationWithUserEndpoint {

        @Test
        @DisplayName("Gets conversation with specific user")
        void getConversationWithUser_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.getConversationBetweenUsers(1L, 2L))
                .thenReturn(testConversationResponse);

            // Act & Assert
            mockMvc.perform(get("/api/conversations/with/2")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));

            verify(conversationService).getConversationBetweenUsers(1L, 2L);
        }

        @Test
        @DisplayName("Returns 404 when no conversation exists")
        void getConversationWithUser_NotFound() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.getConversationBetweenUsers(1L, 3L)).thenReturn(null);

            // Act & Assert
            mockMvc.perform(get("/api/conversations/with/3")
                    .principal(createAuthentication()))
                .andExpect(status().isNotFound());

            verify(conversationService).getConversationBetweenUsers(1L, 3L);
        }
    }

    // ========================================================================
    // CHECK CONVERSATION EXISTS TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/conversations/exists/{userId}")
    class ConversationExistsEndpoint {

        @Test
        @DisplayName("Returns true when conversation exists")
        void conversationExists_True() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.conversationExists(1L, 2L)).thenReturn(true);

            // Act & Assert
            mockMvc.perform(get("/api/conversations/exists/2")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

            verify(conversationService).conversationExists(1L, 2L);
        }

        @Test
        @DisplayName("Returns false when conversation does not exist")
        void conversationExists_False() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(conversationService.conversationExists(1L, 3L)).thenReturn(false);

            // Act & Assert
            mockMvc.perform(get("/api/conversations/exists/3")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

            verify(conversationService).conversationExists(1L, 3L);
        }
    }
}
