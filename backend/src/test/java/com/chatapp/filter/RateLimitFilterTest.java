package com.chatapp.filter;

import com.chatapp.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private Bucket bucket;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(rateLimitConfig);
    }

    @Test
    @DisplayName("Rate Limit: Allows request when bucket has tokens")
    void allowsRequestWhenBucketHasTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(rateLimitConfig.getLoginBucket(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNotNull(filterChain.getRequest()); // Filter chain was invoked
    }

    @Test
    @DisplayName("Rate Limit: Blocks request when bucket is empty")
    void blocksRequestWhenBucketIsEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(rateLimitConfig.getLoginBucket(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Too many requests"));
        assertNull(filterChain.getRequest()); // Filter chain was NOT invoked
    }

    @Test
    @DisplayName("Rate Limit: Uses correct bucket for login endpoint")
    void usesLoginBucketForLoginEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(rateLimitConfig.getLoginBucket(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).getLoginBucket(anyString());
        verify(rateLimitConfig, never()).getRegisterBucket(anyString());
    }

    @Test
    @DisplayName("Rate Limit: Uses correct bucket for register endpoint")
    void usesRegisterBucketForRegisterEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(rateLimitConfig.getRegisterBucket(anyString())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).getRegisterBucket(anyString());
    }

    @Test
    @DisplayName("Rate Limit: Extracts IP from X-Forwarded-For header")
    void extractsIpFromXForwardedFor() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(rateLimitConfig.getLoginBucket("192.168.1.100")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).getLoginBucket("192.168.1.100");
    }
}
