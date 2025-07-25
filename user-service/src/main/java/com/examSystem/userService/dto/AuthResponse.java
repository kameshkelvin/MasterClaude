package com.examSystem.userService.dto;

import com.examSystem.userService.entity.User;
import com.examSystem.userService.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证响应DTO
 * 
 * 基于系统设计文档中的API接口设计
 * 用于返回用户认证成功后的信息和Token
 */
public class AuthResponse {

    private boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo user;
    private LocalDateTime timestamp;

    // 默认构造函数
    public AuthResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 成功响应构造函数
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, User user) {
        this.success = true;
        this.message = "认证成功";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;  
        this.expiresIn = expiresIn;
        this.user = new UserInfo(user);
        this.timestamp = LocalDateTime.now();
    }

    // 失败响应构造函数
    public AuthResponse(String message) {
        this.success = false;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // 静态工厂方法
    public static AuthResponse success(String accessToken, String refreshToken, Long expiresIn, User user) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, user);
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 用户信息内嵌类
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private boolean isActive;
        private boolean emailVerified;
        private List<String> roles;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;

        public UserInfo() {}

        public UserInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();  
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.phone = user.getPhone();
            this.isActive = user.getIsActive();
            this.emailVerified = user.getEmailVerified();
            this.roles = user.getRoles().stream()
                .filter(UserRole::isValid)
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());
            this.createdAt = user.getCreatedAt();
            this.lastLoginAt = user.getLastLoginAt();
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }

        public void setLastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", timestamp=" + timestamp +
                '}';
    }
}