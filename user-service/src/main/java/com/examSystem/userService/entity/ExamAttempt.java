package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试记录实体类
 * 
 * 基于数据库设计文档中的exam_attempts表结构
 * 记录用户的考试尝试和成绩信息
 */
@Entity
@Table(name = "exam_attempts", indexes = {
    @Index(name = "idx_exam_attempts_code", columnList = "attempt_code"),
    @Index(name = "idx_exam_attempts_exam_id", columnList = "exam_id"),
    @Index(name = "idx_exam_attempts_user_id", columnList = "user_id"),
    @Index(name = "idx_exam_attempts_status", columnList = "status"),
    @Index(name = "idx_exam_attempts_started_at", columnList = "started_at"),
    @Index(name = "idx_exam_attempts_submitted_at", columnList = "submitted_at"),
    @Index(name = "idx_exam_attempts_user_exam", columnList = "user_id, exam_id")
})
@EntityListeners(AuditingEntityListener.class)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_code", unique = true, nullable = false, length = 32)
    @NotNull(message = "考试码不能为空")
    private String attemptCode; // 唯一考试码，用于防止重复提交

    @Column(name = "exam_id", nullable = false)
    @NotNull(message = "考试ID不能为空")
    private Long examId;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    // 状态管理
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private AttemptStatus status = AttemptStatus.STARTED;

    // 成绩信息
    @Column(name = "score", precision = 8, scale = 2)
    @DecimalMin(value = "0", message = "成绩不能为负")
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0", message = "百分比不能为负")
    @DecimalMax(value = "100", message = "百分比不能超过100")
    private BigDecimal percentage;

    @Column(name = "grade", length = 5)
    private String grade; // A+, A, B+, B, C+, C, D, F

    @Column(name = "points_earned", precision = 8, scale = 2)
    @DecimalMin(value = "0", message = "获得分数不能为负")
    private BigDecimal pointsEarned = BigDecimal.ZERO;

    @Column(name = "points_possible", precision = 8, scale = 2)
    @DecimalMin(value = "0", message = "总分不能为负")
    private BigDecimal pointsPossible;

    // 时间跟踪
    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @LastModifiedDate
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "time_spent")
    private String timeSpent; // 存储为字符串，如 "01:30:00"

    @Column(name = "remaining_time")
    private Integer remainingTime; // 剩余时间(秒)

    // 环境信息
    @Column(name = "ip_address", nullable = false)
    @NotNull(message = "IP地址不能为空")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "browser_info", columnDefinition = "jsonb")
    private String browserInfo = "{}";

    @Column(name = "screen_resolution", length = 20)
    private String screenResolution;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // 考试行为数据
    @Column(name = "answers_summary", columnDefinition = "jsonb")
    private String answersSummary = "{}"; // 答案统计摘要

    @Column(name = "time_tracking", columnDefinition = "jsonb")
    private String timeTracking = "{}"; // 详细时间跟踪

    @Column(name = "navigation_log", columnDefinition = "jsonb")
    private String navigationLog = "[]"; // 页面导航日志

    // 安全和监控
    @Column(name = "security_flags", columnDefinition = "jsonb")
    private String securityFlags = "[]"; // 安全标记

    @Column(name = "violations_count")
    private Integer violationsCount = 0;

    @Column(name = "proctoring_score", precision = 3, scale = 2)
    @DecimalMin(value = "0", message = "监考评分不能为负")
    @DecimalMax(value = "1", message = "监考评分不能超过1")
    private BigDecimal proctoringScore; // 监考评分(0-1)

    @Column(name = "integrity_verified")
    private Boolean integrityVerified = false;

    // 其他信息
    @Column(name = "attempt_number")
    private Integer attemptNumber = 1; // 第几次尝试

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 备注信息

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}"; // 额外元数据

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", insertable = false, updatable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // 枚举定义
    public enum AttemptStatus {
        STARTED("started", "已开始"),
        IN_PROGRESS("in_progress", "进行中"),
        PAUSED("paused", "已暂停"),
        SUBMITTED("submitted", "已提交"),
        AUTO_SUBMITTED("auto_submitted", "自动提交"),
        GRADED("graded", "已评分"),
        REVIEWED("reviewed", "已审核"),
        CANCELLED("cancelled", "已取消"),
        FLAGGED("flagged", "已标记");

        private final String code;
        private final String description;

        AttemptStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    // 默认构造函数
    public ExamAttempt() {}

    // 构造函数
    public ExamAttempt(String attemptCode, Long examId, Long userId, String ipAddress) {
        this.attemptCode = attemptCode;
        this.examId = examId;
        this.userId = userId;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAttemptCode() {
        return attemptCode;
    }

    public void setAttemptCode(String attemptCode) {
        this.attemptCode = attemptCode;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public BigDecimal getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(BigDecimal pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public BigDecimal getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(String timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Integer getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(Integer remainingTime) {
        this.remainingTime = remainingTime;
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

    public String getBrowserInfo() {
        return browserInfo;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getAnswersSummary() {
        return answersSummary;
    }

    public void setAnswersSummary(String answersSummary) {
        this.answersSummary = answersSummary;
    }

    public String getTimeTracking() {
        return timeTracking;
    }

    public void setTimeTracking(String timeTracking) {
        this.timeTracking = timeTracking;
    }

    public String getNavigationLog() {
        return navigationLog;
    }

    public void setNavigationLog(String navigationLog) {
        this.navigationLog = navigationLog;
    }

    public String getSecurityFlags() {
        return securityFlags;
    }

    public void setSecurityFlags(String securityFlags) {
        this.securityFlags = securityFlags;
    }

    public Integer getViolationsCount() {
        return violationsCount;
    }

    public void setViolationsCount(Integer violationsCount) {
        this.violationsCount = violationsCount;
    }

    public BigDecimal getProctoringScore() {
        return proctoringScore;
    }

    public void setProctoringScore(BigDecimal proctoringScore) {
        this.proctoringScore = proctoringScore;
    }

    public Boolean getIntegrityVerified() {
        return integrityVerified;
    }

    public void setIntegrityVerified(Boolean integrityVerified) {
        this.integrityVerified = integrityVerified;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // 业务方法
    public boolean isCompleted() {
        return AttemptStatus.SUBMITTED.equals(status) || 
               AttemptStatus.AUTO_SUBMITTED.equals(status) ||
               AttemptStatus.GRADED.equals(status) ||
               AttemptStatus.REVIEWED.equals(status);
    }

    public boolean isGraded() {
        return AttemptStatus.GRADED.equals(status) || AttemptStatus.REVIEWED.equals(status);
    }

    public boolean canBeReviewed() {
        return isGraded() && !AttemptStatus.REVIEWED.equals(status);
    }

    public boolean isPassing(BigDecimal passingScore) {
        return percentage != null && passingScore != null && 
               percentage.compareTo(passingScore) >= 0;
    }

    public void submit() {
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public void autoSubmit() {
        this.status = AttemptStatus.AUTO_SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public void grade(BigDecimal score, BigDecimal pointsPossible) {
        this.status = AttemptStatus.GRADED;
        this.score = score;
        this.pointsEarned = score;
        this.pointsPossible = pointsPossible;
        this.gradedAt = LocalDateTime.now();
        
        if (pointsPossible != null && pointsPossible.compareTo(BigDecimal.ZERO) > 0) {
            this.percentage = score.divide(pointsPossible, 2, BigDecimal.ROUND_HALF_UP)
                                  .multiply(BigDecimal.valueOf(100));
        }
    }

    public String getStatusCode() {
        return status != null ? status.getCode() : null;
    }

    public String getStatusDescription() {
        return status != null ? status.getDescription() : null;
    }

    @Override
    public String toString() {
        return "ExamAttempt{" +
                "id=" + id +
                ", attemptCode='" + attemptCode + '\'' +
                ", examId=" + examId +
                ", userId=" + userId +
                ", status=" + status +
                ", score=" + score +
                ", percentage=" + percentage +
                ", startedAt=" + startedAt +
                ", submittedAt=" + submittedAt +
                '}';
    }
}