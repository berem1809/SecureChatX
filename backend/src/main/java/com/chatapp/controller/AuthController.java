package com.chatapp.controller;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints:
 * - POST /api/auth/register : register a new user (sends verification email)
 * - GET  /api/auth/verify   : verify email using token
 * - POST /api/auth/login    : login and receive access + refresh tokens
 * - POST /api/auth/refresh  : exchange refresh token for a new access token
 *
 * Note: client should store access tokens in memory and refresh tokens in
 * HttpOnly cookies for improved security.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Email Verified</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .success { color: green; }
                    </style>
                </head>
                <body>
                    <h1 class="success">Email Verified Successfully!</h1>
                    <p>Your email has been verified. You can now log in to your account.</p>
                    <p><a href="http://localhost:3000/login">Go to Login</a></p>
                </body>
                </html>
                """;
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Verification Failed</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .error { color: red; }
                    </style>
                </head>
                <body>
                    <h1 class="error">Verification Failed</h1>
                    <p>Invalid or expired token. Please try registering again.</p>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().body(html);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse resp = authService.login(request);
        // set refresh token in HttpOnly cookie
        Cookie cookie = new Cookie("refreshToken", resp.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        response.addCookie(cookie);
        // remove refresh from response body to encourage cookie storage
        resp.setRefreshToken(null);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = "refreshToken", required = false) String cookieToken,
                                                @RequestBody(required = false) String refreshToken,
                                                HttpServletResponse response) {
        String token = cookieToken != null ? cookieToken : refreshToken;
        AuthResponse resp = authService.refresh(token);
        // refresh token is re-set into cookie to ensure client cookie stays fresh
        Cookie cookie = new Cookie("refreshToken", resp.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60);
        response.addCookie(cookie);
        resp.setRefreshToken(null);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String cookieToken,
                                    @RequestBody(required = false) String refreshToken,
                                    HttpServletResponse response) {
        String token = cookieToken != null ? cookieToken : refreshToken;
        if (token != null) authService.logout(token);
        // Clear cookie on logout
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
