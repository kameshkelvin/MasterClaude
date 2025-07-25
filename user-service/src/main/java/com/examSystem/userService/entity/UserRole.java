package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户角色实体类
 * 
 * 基于数据库设计文档中的user_roles表结构
 * 支持用户角色管理、权限配置、组织机构关联等功能
 */
@Entity
@Table(name = "user_roles", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_org_role", 
                           columnNames = {"user_id", "organization_id", "role"})
       },
       indexes = {
           @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
           @Index(name = "idx_user_roles_organization_id", columnList = "organization_id"),
           @Index(name = "idx_user_roles_role", columnList = "role"),
           @Index(name = "idx_user_roles_is_active", columnList = "is_active")
       })
@EntityListeners(AuditingEntityListener.class)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "用户不能为空")
    private User user;

    @Column(name = "role", nullable = false, length = 20)
    @NotBlank(message = "角色不能为空")
    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions = "[]";

    @Column(name = "granted_by")
    private Long grantedBy;

    @CreatedDate
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 默认构造函数
    public UserRole() {}

    // 构造函数
    public UserRole(User user, RoleType role, Long organizationId) {
        this.user = user;
        this.role = role;
        this.organizationId = organizationId;
    }

    // 角色类型枚举
    public enum RoleType {
        STUDENT("student", "学生"),
        TEACHER("teacher", "教师"),
        ADMIN("admin", "管理员"),
        SUPER_ADMIN("super_admin", "超级管理员");

        private final String code;
        private final String description;

        RoleType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static RoleType fromCode(String code) {
            for (RoleType type : RoleType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown role code: " + code);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Long getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(Long grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    // 业务方法
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public String getRoleCode() {
        return role != null ? role.getCode() : null;
    }

    public String getRoleDescription() {
        return role != null ? role.getDescription() : null;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", role=" + role +
                ", organizationId=" + organizationId +
                ", isActive=" + isActive +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                '}';
    }
}