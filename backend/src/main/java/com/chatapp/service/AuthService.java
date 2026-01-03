package com.chatapp.service;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.model.User;
import com.chatapp.exception.EmailAlreadyExistsException;
import com.chatapp.exception.InvalidTokenException;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void verifyEmail(String token);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
}

/**
 * Core authentication service implementing registration, email verification,
 * login and refresh token rotation.
 * 
 * Architecture:
 * - MySQL: User data (permanent storage via JPA)
 * - Redis: Verification tokens and refresh tokens (TTL-based, auto-expiring)
 */
@Service
class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationTokenService verificationTokenService,
                           RefreshTokenService refreshTokenService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenService = verificationTokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    @Override
    public void register(RegisterRequest request) {
        logger.info("Register method called for: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        // Create user in MySQL with PENDING_VERIFICATION status
        // No tokens created at this stage - user must verify email first
        User user = new User(
            request.getEmail(), 
            passwordEncoder.encode(request.getPassword()), 
            request.getDisplayName(), 
            "PENDING_VERIFICATION", 
            java.util.List.of("ROLE_USER")
        );
        user = userRepository.save(user);
        logger.info("User saved to MySQL with id: {}", user.getId());

        // Create verification token in Redis with TTL (auto-expires in 24 hours)
        String token = verificationTokenService.createVerificationToken(user.getId());
        logger.info("Verification token created in Redis for user: {}", user.getEmail());

        // Send verification email
        logger.info("Sending verification email to: {}", user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Override
    public void verifyEmail(String token) {
        logger.info("Verify method called with token: {}", token);
        
        // Find userId from Redis verification token
        Long userId = verificationTokenService.findUserIdByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));
        
        // Update user status to ACTIVE in MySQL
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setStatus("ACTIVE");
        userRepository.save(user);
        
        // Delete verification token from Redis
        verificationTokenService.deleteByToken(token);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }

        // Generate tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Store refresh token in Redis (hashed, with TTL)
        // Delete any existing token first to support single-session
        refreshTokenService.deleteByUserId(user.getId());
        refreshTokenService.createRefreshToken(refreshToken, user.getId());

        logger.info("User logged in: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        // Validate JWT signature
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        // Verify token exists in Redis
        Long userId = refreshTokenService.findUserIdByToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found or expired"));
        
        // Get user and generate new access token
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
        
        // Return same refresh token (no rotation on refresh)
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
            logger.info("Refresh token deleted on logout");
        }
    }
}
