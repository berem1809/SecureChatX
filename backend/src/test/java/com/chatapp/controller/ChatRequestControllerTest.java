package com.chatapp.controller;

import com.chatapp.dto.ChatRequestActionRequest;
import com.chatapp.dto.ChatRequestCreateRequest;
import com.chatapp.dto.ChatRequestResponse;
import com.chatapp.dto.UserSearchResponse;
import com.chatapp.model.ChatRequest;
import com.chatapp.model.ChatRequestStatus;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ChatRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatRequestController.
 * Tests all chat request REST endpoints.
 */
class ChatRequestControllerTest {

    @Mock
    private ChatRequestService chatRequestService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private User receiverUser;
    private ChatRequestResponse testChatRequestResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ChatRequestController(chatRequestService, userRepository))
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test users
        testUser = new User("test@example.com", "password", "Test User", "ACTIVE", List.of("ROLE_USER"));
        testUser.setId(1L);

        receiverUser = new User("receiver@example.com", "password", "Receiver User", "ACTIVE", List.of("ROLE_USER"));
        receiverUser.setId(2L);

        // Setup test response using actual DTO structure
        testChatRequestResponse = createTestResponse(ChatRequestStatus.PENDING);
    }

    private ChatRequestResponse createTestResponse(ChatRequestStatus status) {
        // Create a ChatRequest entity
        ChatRequest chatRequest = new ChatRequest(testUser, receiverUser);
        chatRequest.setId(100L);
        chatRequest.setStatus(status);
        return ChatRequestResponse.fromEntity(chatRequest);
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            "test@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ========================================================================
    // CREATE CHAT REQUEST TESTS
    // ========================================================================

    @Nested
    @DisplayName("POST /api/chat-requests")
    class CreateChatRequestEndpoint {

        @Test
        @DisplayName("Successfully creates chat request")
        void createChatRequest_Success() throws Exception {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(2L);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.createChatRequest(eq(1L), any(ChatRequestCreateRequest.class)))
                .thenReturn(testChatRequestResponse);

            // Act & Assert
            mockMvc.perform(post("/api/chat-requests")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.sender.id").value(1L))
                .andExpect(jsonPath("$.receiver.id").value(2L))
                .andExpect(jsonPath("$.status").value("PENDING"));

            verify(chatRequestService).createChatRequest(eq(1L), any(ChatRequestCreateRequest.class));
        }
    }

    // ========================================================================
    // GET CHAT REQUESTS TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/chat-requests/*")
    class GetChatRequestsEndpoints {

        @Test
        @DisplayName("Gets sent requests")
        void getSentRequests_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.getSentRequests(1L)).thenReturn(List.of(testChatRequestResponse));

            // Act & Assert
            mockMvc.perform(get("/api/chat-requests/sent")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sender.id").value(1L));

            verify(chatRequestService).getSentRequests(1L);
        }

        @Test
        @DisplayName("Gets received requests")
        void getReceivedRequests_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.getReceivedRequests(1L)).thenReturn(List.of(testChatRequestResponse));

            // Act & Assert
            mockMvc.perform(get("/api/chat-requests/received")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

            verify(chatRequestService).getReceivedRequests(1L);
        }

        @Test
        @DisplayName("Gets pending requests")
        void getPendingRequests_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.getPendingRequests(1L)).thenReturn(List.of(testChatRequestResponse));

            // Act & Assert
            mockMvc.perform(get("/api/chat-requests/pending")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

            verify(chatRequestService).getPendingRequests(1L);
        }

        @Test
        @DisplayName("Gets specific chat request")
        void getChatRequest_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.getChatRequest(100L, 1L)).thenReturn(testChatRequestResponse);

            // Act & Assert
            mockMvc.perform(get("/api/chat-requests/100")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));

            verify(chatRequestService).getChatRequest(100L, 1L);
        }
    }

    // ========================================================================
    // CHAT REQUEST ACTION TESTS
    // ========================================================================

    @Nested
    @DisplayName("POST /api/chat-requests/{id}/action")
    class ChatRequestActionEndpoint {

        @Test
        @DisplayName("Accepts chat request")
        void acceptChatRequest_Success() throws Exception {
            // Arrange
            ChatRequestActionRequest actionRequest = new ChatRequestActionRequest();
            actionRequest.setAction("ACCEPT");

            ChatRequestResponse acceptedResponse = createTestResponse(ChatRequestStatus.ACCEPTED);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.acceptChatRequest(100L, 1L)).thenReturn(acceptedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/chat-requests/100/action")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

            verify(chatRequestService).acceptChatRequest(100L, 1L);
        }

        @Test
        @DisplayName("Rejects chat request")
        void rejectChatRequest_Success() throws Exception {
            // Arrange
            ChatRequestActionRequest actionRequest = new ChatRequestActionRequest();
            actionRequest.setAction("REJECT");

            ChatRequestResponse rejectedResponse = createTestResponse(ChatRequestStatus.REJECTED);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(chatRequestService.rejectChatRequest(100L, 1L)).thenReturn(rejectedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/chat-requests/100/action")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(chatRequestService).rejectChatRequest(100L, 1L);
        }
    }
}
