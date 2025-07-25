package com.examSystem.userService.controller;

import com.examSystem.userService.config.JwtConfig;
import com.examSystem.userService.dto.AuthResponse;
import com.examSystem.userService.dto.LoginRequest;
import com.examSystem.userService.dto.RegisterRequest;
import com.examSystem.userService.entity.User;
import com.examSystem.userService.entity.UserRole;
import com.examSystem.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证控制器
 * 
 * 基于系统设计文档中的API接口设计
 * 提供用户注册、登录、Token刷新等认证相关接口
 */
@Tag(name = "认证管理", description = "用户认证相关API，包括注册、登录、Token管理等")
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @param bindingResult 验证结果
     * @param request HTTP请求
     * @return 注册结果
     */
    @Operation(
        summary = "用户注册",
        description = "创建新用户账户，支持用户名、邮箱、密码等基本信息注册"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "参数验证失败或用户已存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        
        logger.info("User registration attempt: username={}, email={}, IP={}", 
            registerRequest.getUsername(), registerRequest.getEmail(), getClientIp(request));

        try {
            // 验证请求参数
            if (bindingResult.hasErrors()) {
                Map<String, Object> errors = buildValidationErrors(bindingResult);
                return ResponseEntity.badRequest().body(buildErrorResponse("参数验证失败", errors));
            }

            // 验证密码确认
            if (!registerRequest.isPasswordMatched()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("密码与确认密码不匹配", null));
            }

            // 执行注册
            User user = userService.register(
                registerRequest.getUsername(),
                registerRequest.getEmail(), 
                registerRequest.getPassword(),
                registerRequest.getPhone()
            );

            // 更新用户其他信息
            if (registerRequest.getFirstName() != null || registerRequest.getLastName() != null) {
                user.setFirstName(registerRequest.getFirstName());
                user.setLastName(registerRequest.getLastName());
                userService.updateUser(user.getId(), user);
            }

            logger.info("User registered successfully: userId={}, username={}", 
                user.getId(), user.getUsername());

            // 构建成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功，请验证邮箱后登录");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("emailVerified", user.getEmailVerified());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed due to validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage(), null));
            
        } catch (Exception e) {
            logger.error("Registration failed for username: {}", registerRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("注册失败，请稍后重试", null));
        }
    }

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @param bindingResult 验证结果
     * @param request HTTP请求
     * @return 登录结果和Token
     */
    @Operation(
        summary = "用户登录",
        description = "用户身份认证，成功后返回JWT访问令牌和刷新令牌"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功，返回Token"),
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                 BindingResult bindingResult,
                                 HttpServletRequest request) {
        
        logger.info("User login attempt: username={}, IP={}, deviceType={}", 
            loginRequest.getUsername(), getClientIp(request), loginRequest.getDeviceType());

        try {
            // 验证请求参数
            if (bindingResult.hasErrors()) {
                Map<String, Object> errors = buildValidationErrors(bindingResult);
                return ResponseEntity.badRequest().body(buildErrorResponse("参数验证失败", errors));
            }

            // 用户认证
            User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

            // 获取用户角色
            List<String> roles = user.getRoles().stream()
                .filter(UserRole::isValid)
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());

            // 生成JWT Token
            String accessToken = jwtConfig.generateAccessToken(
                user.getId(),
                user.getUsername(),
                roles,
                getUserOrganizationId(user) // 获取用户所属组织ID
            );

            String refreshToken = jwtConfig.generateRefreshToken(
                user.getId(),
                user.getUsername()
            );

            // 更新最后登录时间
            userService.updateLastLoginTime(user.getId());

            // TODO: 创建用户会话记录
            // sessionService.createSession(user.getId(), getClientIp(request), 
            //     getUserAgent(request), loginRequest.getDeviceType());

            logger.info("User logged in successfully: userId={}, username={}", 
                user.getId(), user.getUsername());

            // 构建响应
            AuthResponse authResponse = AuthResponse.success(
                accessToken,
                refreshToken,
                jwtConfig.getJwtExpiration(),
                user
            );

            return ResponseEntity.ok(authResponse);

        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for username: {} - {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.failure("用户名或密码错误"));
            
        } catch (Exception e) {
            logger.error("Login failed for username: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.failure("登录失败，请稍后重试"));
        }
    }

    /**
     * 刷新Token
     * 
     * @param request HTTP请求
     * @return 新的Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String refreshToken = extractTokenFromRequest(request);
        
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(AuthResponse.failure("缺少refresh token"));
        }

        try {
            // 验证refresh token
            if (!jwtConfig.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.failure("Invalid refresh token"));
            }

            // 从token中获取用户信息
            String username = jwtConfig.getUsernameFromToken(refreshToken);
            Long userId = jwtConfig.getUserIdFromToken(refreshToken);

            // 查找用户
            User user = userService.findByUsername(username);
            if (user == null || user.getIsDeleted() || !user.getIsActive()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.failure("用户不存在或已禁用"));
            }

            // 获取用户角色
            List<String> roles = user.getRoles().stream()
                .filter(UserRole::isValid)
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());

            // 生成新的access token
            String newAccessToken = jwtConfig.generateAccessToken(
                user.getId(),
                user.getUsername(),
                roles,
                getUserOrganizationId(user)
            );

            // 生成新的refresh token
            String newRefreshToken = jwtConfig.generateRefreshToken(
                user.getId(),
                user.getUsername()
            );

            logger.info("Token refreshed successfully for user: {}", username);

            AuthResponse authResponse = AuthResponse.success(
                newAccessToken,
                newRefreshToken,
                jwtConfig.getJwtExpiration(),
                user
            );

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.failure("Token刷新失败"));
        }
    }

    /**
     * 登出
     * 
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        try {
            if (token != null) {
                String username = jwtConfig.getUsernameFromToken(token);
                
                // TODO: 将token加入黑名单
                // tokenBlacklistService.addToBlacklist(token);
                
                // TODO: 清除用户会话
                // sessionService.invalidateUserSessions(username);
                
                logger.info("User logged out successfully: {}", username);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登出成功");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.ok(buildErrorResponse("登出失败", null));
        }
    }

    /**
     * 验证Token状态
     * 
     * @param request HTTP请求
     * @return Token验证结果
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse("缺少访问令牌", null));
        }

        try {
            String username = jwtConfig.getUsernameFromToken(token);
            
            if (jwtConfig.validateToken(token, username)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Token有效");
                response.put("username", username);
                response.put("userId", jwtConfig.getUserIdFromToken(token));
                response.put("roles", jwtConfig.getRolesFromToken(token));
                response.put("remainingTime", jwtConfig.getTokenRemainingTime(token));
                response.put("isNearExpiry", jwtConfig.isTokenNearExpiry(token));
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("Token已失效", null));
            }

        } catch (Exception e) {
            logger.error("Token verification failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse("Token验证失败", null));
        }
    }

    // 辅助方法

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 获取用户代理
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * 获取用户所属组织ID
     */
    private Long getUserOrganizationId(User user) {
        return user.getRoles().stream()
            .filter(UserRole::isValid)
            .map(UserRole::getOrganizationId)
            .findFirst()
            .orElse(null);
    }

    /**
     * 构建验证错误信息
     */
    private Map<String, Object> buildValidationErrors(BindingResult bindingResult) {
        Map<String, Object> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(String message, Map<String, Object> details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        
        if (details != null) {
            response.put("errors", details);
        }
        
        return response;
    }
}