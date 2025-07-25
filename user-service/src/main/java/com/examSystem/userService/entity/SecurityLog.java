package com.examSystem.userService.entity;

import com.examSystem.userService.service.security.ExamSecurityService.SecurityLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 安全日志实体类
 * 
 * 记录考试过程中的安全事件和违规行为
 */
@Entity
@Table(name = "security_logs")
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_level", nullable = false)
    private SecurityLevel securityLevel;

    @Column(name = "client_ip", length = 45)
    private String clientIP;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    // 构造函数
    public SecurityLog() {
        this.createdAt = LocalDateTime.now();
    }

    public SecurityLog(Long studentId, Long attemptId, String eventType, 
                      String description, SecurityLevel securityLevel) {
        this();
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.eventType = eventType;
        this.description = description;
        this.securityLevel = securityLevel;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    @Override
    public String toString() {
        return "SecurityLog{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", attemptId=" + attemptId +
                ", eventType='" + eventType + '\'' +
                ", description='" + description + '\'' +
                ", securityLevel=" + securityLevel +
                ", createdAt=" + createdAt +
                '}';
    }
}