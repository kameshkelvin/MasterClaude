package com.examSystem.userService.controller;

import com.examSystem.userService.config.JwtConfig;
import com.examSystem.userService.dto.LoginRequest;
import com.examSystem.userService.dto.RegisterRequest;
import com.examSystem.userService.entity.User;
import com.examSystem.userService.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器单元测试
 * 
 * 基于系统设计文档中的测试架构
 * 测试用户注册、登录等API接口
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setIsDeleted(false);
        testUser.setEmailVerified(false);

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("TestPassword123");
        registerRequest.setConfirmPassword("TestPassword123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("TestPassword123");
    }

    @Test
    void testRegister_Success() throws Exception {
        // Given
        when(userService.register(anyString(), anyString(), anyString(), any())).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功，请验证邮箱后登录"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).register("testuser", "test@example.com", "TestPassword123", null);
    }

    @Test
    void testRegister_ValidationError() throws Exception {
        // Given
        registerRequest.setUsername(""); // Invalid username

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("参数验证失败"));

        verify(userService, never()).register(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testRegister_PasswordMismatch() throws Exception {
        // Given
        registerRequest.setConfirmPassword("DifferentPassword123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("密码与确认密码不匹配"));

        verify(userService, never()).register(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testRegister_UserAlreadyExists() throws Exception {
        // Given
        when(userService.register(anyString(), anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("用户名已存在"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given
        when(userService.authenticate("testuser", "TestPassword123")).thenReturn(testUser);
        when(jwtConfig.generateAccessToken(anyLong(), anyString(), anyList(), any()))
                .thenReturn("access_token");
        when(jwtConfig.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("refresh_token");
        when(jwtConfig.getJwtExpiration()).thenReturn(3600000L);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("认证成功"))
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(userService).authenticate("testuser", "TestPassword123");
        verify(userService).updateLastLoginTime(1L);
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Given
        when(userService.authenticate("testuser", "TestPassword123"))
                .thenThrow(new IllegalArgumentException("用户名或密码错误"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpected(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void testLogin_ValidationError() throws Exception {
        // Given
        loginRequest.setUsername(""); // Invalid username

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("参数验证失败"));

        verify(userService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // Given
        String refreshToken = "valid_refresh_token";
        when(jwtConfig.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtConfig.getUsernameFromToken(refreshToken)).thenReturn("testuser");
        when(jwtConfig.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(jwtConfig.generateAccessToken(anyLong(), anyString(), anyList(), any()))
                .thenReturn("new_access_token");
        when(jwtConfig.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("new_refresh_token");
        when(jwtConfig.getJwtExpiration()).thenReturn(3600000L);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf())
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").value("new_access_token"))
                .andExpect(jsonPath("$.refreshToken").value("new_refresh_token"));
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        // Given
        String refreshToken = "invalid_refresh_token";
        when(jwtConfig.validateRefreshToken(refreshToken)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf())
                .header("Authorization", "Bearer " + refreshToken))
                .andExpected(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void testRefreshToken_MissingToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("缺少refresh token"));
    }

    @Test
    @WithMockUser
    void testLogout_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(csrf()))
                .andExpect(status().isOk())  
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("登出成功"));
    }

    @Test
    void testVerifyToken_Success() throws Exception {
        // Given
        String token = "valid_token";
        when(jwtConfig.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtConfig.validateToken(token, "testuser")).thenReturn(true);
        when(jwtConfig.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtConfig.getRolesFromToken(token)).thenReturn(java.util.List.of("STUDENT"));
        when(jwtConfig.getTokenRemainingTime(token)).thenReturn(3600000L);
        when(jwtConfig.isTokenNearExpiry(token)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/verify")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token有效"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testVerifyToken_InvalidToken() throws Exception {
        // Given
        String token = "invalid_token";
        when(jwtConfig.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtConfig.validateToken(token, "testuser")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/verify")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token已失效"));
    }

    @Test
    void testVerifyToken_MissingToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/auth/verify"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("缺少访问令牌"));
    }
}