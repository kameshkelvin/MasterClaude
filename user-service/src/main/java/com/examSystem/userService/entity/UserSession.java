package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 
 * 基于数据库设计文档中的user_sessions表结构
 * 支持用户会话管理、设备跟踪、安全监控等功能
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_device_type", columnList = "device_type"),
    @Index(name = "idx_user_sessions_is_active", columnList = "is_active"),
    @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at"),
    @Index(name = "idx_user_sessions_last_activity", columnList = "last_activity")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSession {

    @Id
    @Column(name = "session_id", length = 128)
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "用户不能为空")
    private User user;

    @Column(name = "device_type", length = 20)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType = DeviceType.WEB;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "ip_address", nullable = false)
    @NotBlank(message = "IP地址不能为空")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "location_country", length = 2)
    private String locationCountry;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expiresAt;

    @LastModifiedDate
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    // 默认构造函数
    public UserSession() {}

    // 构造函数
    public UserSession(String sessionId, User user, String ipAddress, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.user = user;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.lastActivity = LocalDateTime.now();
    }

    // 设备类型枚举
    public enum DeviceType {
        WEB("web", "Web浏览器"),
        MOBILE("mobile", "移动设备"),
        TABLET("tablet", "平板设备"),
        DESKTOP("desktop", "桌面应用");

        private final String code;
        private final String description;

        DeviceType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static DeviceType fromCode(String code) {
            for (DeviceType type : DeviceType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return WEB; // 默认返回WEB类型
        }
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    // 业务方法
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public void invalidate() {
        this.isActive = false;
    }

    public long getSessionDurationMinutes() {
        if (createdAt != null && lastActivity != null) {
            return java.time.Duration.between(createdAt, lastActivity).toMinutes();
        }
        return 0;
    }

    public String getDeviceTypeCode() {
        return deviceType != null ? deviceType.getCode() : null;
    }

    public String getDeviceTypeDescription() {
        return deviceType != null ? deviceType.getDescription() : null;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId=" + (user != null ? user.getId() : null) +
                ", deviceType=" + deviceType +
                ", ipAddress='" + ipAddress + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", lastActivity=" + lastActivity +
                '}';
    }
}