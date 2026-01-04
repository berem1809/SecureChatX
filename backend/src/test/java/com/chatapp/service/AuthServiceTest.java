package com.chatapp.service;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.exception.EmailAlreadyExistsException;
import com.chatapp.exception.InvalidTokenException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
            userRepository,
            verificationTokenService,
            refreshTokenService,
            passwordEncoder,
            jwtTokenProvider,
            emailService
        );
    }

    // ============ REGISTRATION TESTS ============

    @Test
    @DisplayName("Register: Successfully registers new user")
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setDisplayName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(verificationTokenService.createVerificationToken(1L)).thenReturn("verification-token");

        assertDoesNotThrow(() -> authService.register(request));

        verify(userRepository).save(any(User.class));
        verify(verificationTokenService).createVerificationToken(1L);
        verify(emailService).sendVerificationEmail("test@example.com", "verification-token");
    }

    @Test
    @DisplayName("Register: Throws exception for duplicate email")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // ============ EMAIL VERIFICATION TESTS ============

    @Test
    @DisplayName("Verify Email: Successfully verifies valid token")
    void verifyEmail_Success() {
        User user = new User("test@example.com", "password", "Test", "PENDING_VERIFICATION", List.of("ROLE_USER"));
        user.setId(1L);

        when(verificationTokenService.findUserIdByToken("valid-token")).thenReturn(Optional.of(1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> authService.verifyEmail("valid-token"));

        assertEquals("ACTIVE", user.getStatus());
        verify(userRepository).save(user);
        verify(verificationTokenService).deleteByToken("valid-token");
    }

    @Test
    @DisplayName("Verify Email: Throws exception for invalid token")
    void verifyEmail_InvalidToken_ThrowsException() {
        when(verificationTokenService.findUserIdByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.verifyEmail("invalid-token"));
    }

    // ============ LOGIN TESTS ============

    @Test
    @DisplayName("Login: Successfully logs in verified user")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = new User("test@example.com", "encodedPassword", "Test", "ACTIVE", List.of("ROLE_USER"));
        user.setId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("test@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenService).deleteByUserId(1L);
        verify(refreshTokenService).createRefreshToken("refresh-token", 1L);
    }

    @Test
    @DisplayName("Login: Throws exception for non-existent user")
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login: Throws exception for wrong password")
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        User user = new User("test@example.com", "encodedPassword", "Test", "ACTIVE", List.of("ROLE_USER"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login: Throws exception for unverified user")
    void login_UnverifiedUser_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = new User("test@example.com", "encodedPassword", "Test", "PENDING_VERIFICATION", List.of("ROLE_USER"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Email not verified"));
    }

    // ============ REFRESH TOKEN TESTS ============

    @Test
    @DisplayName("Refresh: Successfully refreshes access token")
    void refresh_Success() {
        User user = new User("test@example.com", "password", "Test", "ACTIVE", List.of("ROLE_USER"));
        user.setId(1L);

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(refreshTokenService.findUserIdByToken("valid-refresh-token")).thenReturn(Optional.of(1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyString(), any())).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("valid-refresh-token");

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    @DisplayName("Refresh: Throws exception for invalid JWT signature")
    void refresh_InvalidSignature_ThrowsException() {
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refresh("invalid-token"));
    }

    @Test
    @DisplayName("Refresh: Throws exception for expired/revoked token")
    void refresh_TokenNotInRedis_ThrowsException() {
        when(jwtTokenProvider.validateToken("expired-token")).thenReturn(true);
        when(refreshTokenService.findUserIdByToken("expired-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.refresh("expired-token"));
    }

    // ============ LOGOUT TESTS ============

    @Test
    @DisplayName("Logout: Successfully invalidates refresh token")
    void logout_Success() {
        authService.logout("refresh-token");
        verify(refreshTokenService).deleteByToken("refresh-token");
    }

    @Test
    @DisplayName("Logout: Handles null token gracefully")
    void logout_NullToken_NoException() {
        assertDoesNotThrow(() -> authService.logout(null));
        verify(refreshTokenService, never()).deleteByToken(any());
    }
}
