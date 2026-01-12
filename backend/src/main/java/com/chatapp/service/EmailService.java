package com.chatapp.service;

// ============================================================================
// IMPORTS
// ============================================================================

// Logging framework (SLF4J with Logback implementation)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring Mail classes for sending emails
import org.springframework.mail.SimpleMailMessage;  // Simple text email (no HTML)
import org.springframework.mail.javamail.JavaMailSender;  // Email sending interface

import org.springframework.stereotype.Service;  // Marks this as a Spring service

/**
 * ============================================================================
 * EMAIL SERVICE INTERFACE - Contract for email operations
 * ============================================================================
 * 
 * WHY USE AN INTERFACE?
 * - Testability: Easy to mock in unit tests
 * - Flexibility: Could swap implementations (e.g., SendGrid, AWS SES)
 * - Abstraction: Hides email provider details from callers
 */
public interface EmailService {
    
    /**
     * Sends a verification email to the user.
     * 
     * @param to Recipient email address
     * @param token Verification token to include in link
     */
    void sendVerificationEmail(String to, String token);
}

/**
 * ============================================================================
 * EMAIL SERVICE IMPLEMENTATION - Sends emails via Gmail SMTP
 * ============================================================================
 * 
 * HOW EMAIL SENDING WORKS:
 * ------------------------
 * 
 * 1. Spring Boot auto-configures JavaMailSender from application.properties:
 *    - spring.mail.host=smtp.gmail.com
 *    - spring.mail.port=587
 *    - spring.mail.username=your-email@gmail.com
 *    - spring.mail.password=your-app-password
 * 
 * 2. This service uses JavaMailSender to send emails
 * 
 * 3. Gmail SMTP server delivers the email to recipient
 * 
 * GMAIL APP PASSWORD:
 * -------------------
 * You CAN'T use your regular Gmail password! Google blocks it.
 * Instead, you need an "App Password":
 * 
 * 1. Go to: https://myaccount.google.com/apppasswords
 * 2. Sign in to your Gmail account
 * 3. Enable 2-Factor Authentication (required)
 * 4. Generate a new App Password for "Mail"
 * 5. Copy the 16-character password
 * 6. Use it in application.properties as spring.mail.password
 * 
 * EMAIL VERIFICATION FLOW:
 * ------------------------
 * 1. User registers with email
 * 2. AuthService calls this service with email + token
 * 3. We construct email with verification link
 * 4. JavaMailSender sends via Gmail SMTP
 * 5. User receives email with clickable link
 * 6. User clicks: /api/auth/verify?token=550e8400...
 * 7. AuthService verifies token and activates account
 * 
 * WHAT IS SMTP?
 * -------------
 * SMTP = Simple Mail Transfer Protocol
 * It's the standard protocol for sending emails across the internet.
 * 
 * PORT 587: Used for TLS-encrypted connections (secure)
 * PORT 465: Used for SSL connections (older, less common)
 * PORT 25: Plain text (insecure, often blocked)
 * 
 * @see AuthService Where email is triggered during registration
 */
@Service  // Marks this as a Spring-managed service bean
class EmailServiceImpl implements EmailService {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Logger for debugging email sending */
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    /**
     * JavaMailSender - Spring's interface for sending emails.
     * 
     * Spring Boot auto-configures this bean based on properties:
     * - spring.mail.host (smtp.gmail.com)
     * - spring.mail.port (587)
     * - spring.mail.username (your Gmail)
     * - spring.mail.password (your App Password)
     * - spring.mail.properties.mail.smtp.auth=true
     * - spring.mail.properties.mail.smtp.starttls.enable=true
     */
    private final JavaMailSender mailSender;

    /**
     * Constructor injection - Spring provides the mail sender.
     * Logs when service is initialized to confirm email is configured.
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.info("EmailService initialized with JavaMailSender");
    }

    // ========================================================================
    // SEND VERIFICATION EMAIL
    // ========================================================================

    /**
     * Sends a verification email to the user.
     * 
     * EMAIL CONTENTS:
     * - From: piranaberem14@gmail.com (configured sender)
     * - To: User's email
     * - Subject: "Verify Your Email - ChatApp"
     * - Body: Plain text with verification link
     * 
     * LINK FORMAT:
     * http://localhost:8080/api/auth/verify?token={uuid}
     * 
     * In production, you'd use your real domain:
     * https://yourchatapp.com/api/auth/verify?token={uuid}
     * 
     * @param to Recipient email address (user's email)
     * @param token Verification token (UUID string)
     */
    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            logger.info("Attempting to send verification email to: {}", to);
            
            // Create a simple text email (no HTML)
            SimpleMailMessage message = new SimpleMailMessage();
            
            // Set sender (must match spring.mail.username or be authorized)
            message.setFrom("piranaberem14@gmail.com");
            
            // Set recipient (user's email)
            message.setTo(to);
            
            // Set email subject (appears in inbox)
            message.setSubject("Verify Your Email - ChatApp");
            
            // Set email body (plain text)
            // Contains the verification link the user clicks
            message.setText(
                "Welcome to ChatApp!\n\n" +
                "Please verify your email by clicking the link below:\n\n" +
                "http://localhost:8080/api/auth/verify?token=" + token + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create this account, you can ignore this email."
            );
            
            // Send the email via Gmail SMTP
            // This is a blocking call - waits for SMTP server response
            mailSender.send(message);
            
            logger.info("Verification email sent successfully to: {}", to);
            
        } catch (Exception e) {
            // Log error but don't throw - user can request new verification email
            // Common errors:
            // - AuthenticationFailedException: Wrong Gmail credentials
            // - MailSendException: SMTP server unreachable
            // - AddressException: Invalid recipient email format
            logger.error("Failed to send verification email to: {} - Error: {}", to, e.getMessage(), e);
            
            /*
             * IMPROVEMENT IDEAS:
             * 
             * 1. Retry logic: Try again after 30 seconds
             * 
             * 2. Queue emails: Use a message queue (RabbitMQ, Kafka)
             *    for reliable delivery
             * 
             * 3. Throw exception: Let caller handle failure
             *    throw new RuntimeException("Failed to send email", e);
             * 
             * 4. Use async: @Async to send in background thread
             *    - User doesn't wait for email to send
             *    - Registration completes faster
             */
        }
    }
    
    /*
     * FUTURE IMPROVEMENTS:
     * ====================
     * 
     * 1. HTML emails with better formatting:
     *    - Use MimeMessage instead of SimpleMailMessage
     *    - Include logo, styled button, footer
     * 
     * 2. Template engine (Thymeleaf):
     *    - Store email templates as HTML files
     *    - Replace placeholders with user data
     * 
     * 3. Email tracking:
     *    - Track opens (pixel tracking)
     *    - Track clicks (redirect links)
     * 
     * 4. Other email types:
     *    - Password reset emails
     *    - Welcome emails after verification
     *    - Notification emails
     */
}
