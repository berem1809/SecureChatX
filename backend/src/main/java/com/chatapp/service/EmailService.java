package com.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
}

@Service
class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.info("EmailService initialized with JavaMailSender");
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            logger.info("Attempting to send verification email to: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("piranaberem14@gmail.com");
            message.setTo(to);
            message.setSubject("Verify Your Email - ChatApp");
            message.setText("Please verify your email by clicking the link:\n\n" +
                "http://localhost:8080/api/auth/verify?token=" + token + "\n\n" +
                "This link will expire in 24 hours.");
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {} - Error: {}", to, e.getMessage(), e);
        }
    }
}
