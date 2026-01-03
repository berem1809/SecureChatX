package com.chatapp.controller;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import com.chatapp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void logoutEndpointCallsService() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", "token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).logout("token");
    }

    @Test
    void loginSetsHttpOnlyCookie() throws Exception {
        ObjectMapper om = new ObjectMapper();
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("password");

        when(authService.login(org.mockito.ArgumentMatchers.any())).thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login").content(om.writeValueAsString(req)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService).login(org.mockito.ArgumentMatchers.any());
    }
}
