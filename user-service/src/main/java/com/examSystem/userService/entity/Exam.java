package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试实体类
 * 
 * 基于数据库设计文档中的exams表结构
 * 支持各种考试类型和配置选项
 */
@Entity
@Table(name = "exams", indexes = {
    @Index(name = "idx_exams_code", columnList = "code"),
    @Index(name = "idx_exams_course_id", columnList = "course_id"),
    @Index(name = "idx_exams_created_by", columnList = "created_by"),
    @Index(name = "idx_exams_type", columnList = "type"),
    @Index(name = "idx_exams_status", columnList = "status"),
    @Index(name = "idx_exams_available_from", columnList = "available_from"),
    @Index(name = "idx_exams_available_until", columnList = "available_until"),
    @Index(name = "idx_exams_published_at", columnList = "published_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "考试标题不能为空")
    @Size(max = 255, message = "考试标题长度不能超过255字符")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    @NotBlank(message = "考试代码不能为空")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "考试代码只能包含大写字母、数字、下划线和横线")
    private String code;

    @Column(name = "course_id", nullable = false)
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @Column(name = "created_by", nullable = false)
    @NotNull(message = "创建者ID不能为空")
    private Long createdBy;

    @Column(name = "type", nullable = false, length = 20)
    @NotNull(message = "考试类型不能为空")
    @Enumerated(EnumType.STRING)
    private ExamType type;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private ExamStatus status = ExamStatus.DRAFT;

    // 考试配置
    @Column(name = "duration_minutes", nullable = false)
    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于0分钟")
    @Max(value = 1440, message = "考试时长不能超过1440分钟(24小时)")
    private Integer durationMinutes;

    @Column(name = "max_attempts")
    @Min(value = 1, message = "最大尝试次数必须大于0")
    @Max(value = 10, message = "最大尝试次数不能超过10次")
    private Integer maxAttempts = 1;

    @Column(name = "passing_score", precision = 5, scale = 2, nullable = false)
    @NotNull(message = "及格分数不能为空")
    @DecimalMin(value = "0", message = "及格分数不能为负")
    @DecimalMax(value = "100", message = "及格分数不能超过100")
    private BigDecimal passingScore;

    @Column(name = "total_points", precision = 8, scale = 2)
    @DecimalMin(value = "0", message = "总分不能为负")
    private BigDecimal totalPoints = BigDecimal.ZERO;

    // 时间控制
    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_until")
    private LocalDateTime availableUntil;

    @Column(name = "late_submission_penalty", precision = 3, scale = 2)
    @DecimalMin(value = "0", message = "迟交扣分比例不能为负")
    @DecimalMax(value = "1", message = "迟交扣分比例不能超过1")
    private BigDecimal lateSubmissionPenalty = BigDecimal.ZERO;

    @Column(name = "grace_period_minutes")
    @Min(value = 0, message = "宽限期不能为负")
    private Integer gracePeriodMinutes = 0;

    // 考试设置(JSON格式存储)
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings = "{}"; // 通用设置

    @Column(name = "proctoring_config", columnDefinition = "jsonb")
    private String proctoringConfig = "{}"; // 监考配置

    @Column(name = "grading_config", columnDefinition = "jsonb")
    private String gradingConfig = "{}"; // 评分配置

    // 题目设置
    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = false;

    @Column(name = "shuffle_options")
    private Boolean shuffleOptions = false;

    @Column(name = "show_results_immediately")
    private Boolean showResultsImmediately = true;

    @Column(name = "allow_review")
    private Boolean allowReview = true;

    @Column(name = "show_correct_answers")
    private Boolean showCorrectAnswers = false;

    // 访问控制
    @Column(name = "password_protected")
    private Boolean passwordProtected = false;

    @Column(name = "exam_password")
    private String examPassword;

    @Column(name = "ip_restrictions")
    private String ipRestrictions; // 存储IP地址数组的JSON字符串

    @Column(name = "browser_lockdown")
    private Boolean browserLockdown = false;

    // 统计字段（冗余存储，提高查询性能）
    @Column(name = "questions_count")
    private Integer questionsCount = 0;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @Column(name = "completed_attempts")
    private Integer completedAttempts = 0;

    @Column(name = "avg_score", precision = 5, scale = 2)
    private BigDecimal avgScore;

    @Column(name = "avg_duration")
    private String avgDuration; // 存储为字符串，如 "01:30:00"

    // 时间戳
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "first_attempt_at")
    private LocalDateTime firstAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关联关系
    @OneToMany(mappedBy = "examId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    // 枚举定义
    public enum ExamType {
        QUIZ("quiz", "小测验"),
        ASSIGNMENT("assignment", "作业"),
        MIDTERM("midterm", "期中考试"),
        FINAL("final", "期末考试"),
        CERTIFICATION("certification", "认证考试"),
        PRACTICE("practice", "练习测试");

        private final String code;
        private final String description;

        ExamType(String code, String description) {
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

    public enum ExamStatus {
        DRAFT("draft", "草稿"),
        REVIEW("review", "审核中"),
        PUBLISHED("published", "已发布"),
        ACTIVE("active", "进行中"),
        COMPLETED("completed", "已结束"),
        ARCHIVED("archived", "已归档");

        private final String code;
        private final String description;

        ExamStatus(String code, String description) {
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
    public Exam() {}

    // 构造函数
    public Exam(String title, String code, Long courseId, Long createdBy, ExamType type, Integer durationMinutes, BigDecimal passingScore) {
        this.title = title;
        this.code = code;
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.passingScore = passingScore;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public ExamType getType() {
        return type;
    }

    public void setType(ExamType type) {
        this.type = type;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public BigDecimal getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(BigDecimal totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public BigDecimal getLateSubmissionPenalty() {
        return lateSubmissionPenalty;
    }

    public void setLateSubmissionPenalty(BigDecimal lateSubmissionPenalty) {
        this.lateSubmissionPenalty = lateSubmissionPenalty;
    }

    public Integer getGracePeriodMinutes() {
        return gracePeriodMinutes;
    }

    public void setGracePeriodMinutes(Integer gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public String getProctoringConfig() {
        return proctoringConfig;
    }

    public void setProctoringConfig(String proctoringConfig) {
        this.proctoringConfig = proctoringConfig;
    }

    public String getGradingConfig() {
        return gradingConfig;
    }

    public void setGradingConfig(String gradingConfig) {
        this.gradingConfig = gradingConfig;
    }

    public Boolean getShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(Boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public Boolean getShuffleOptions() {
        return shuffleOptions;
    }

    public void setShuffleOptions(Boolean shuffleOptions) {
        this.shuffleOptions = shuffleOptions;
    }

    public Boolean getShowResultsImmediately() {
        return showResultsImmediately;
    }

    public void setShowResultsImmediately(Boolean showResultsImmediately) {
        this.showResultsImmediately = showResultsImmediately;
    }

    public Boolean getAllowReview() {
        return allowReview;
    }

    public void setAllowReview(Boolean allowReview) {
        this.allowReview = allowReview;
    }

    public Boolean getShowCorrectAnswers() {
        return showCorrectAnswers;
    }

    public void setShowCorrectAnswers(Boolean showCorrectAnswers) {
        this.showCorrectAnswers = showCorrectAnswers;
    }

    public Boolean getPasswordProtected() {
        return passwordProtected;
    }

    public void setPasswordProtected(Boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }

    public String getExamPassword() {
        return examPassword;
    }

    public void setExamPassword(String examPassword) {
        this.examPassword = examPassword;
    }

    public String getIpRestrictions() {
        return ipRestrictions;
    }

    public void setIpRestrictions(String ipRestrictions) {
        this.ipRestrictions = ipRestrictions;
    }

    public Boolean getBrowserLockdown() {
        return browserLockdown;
    }

    public void setBrowserLockdown(Boolean browserLockdown) {
        this.browserLockdown = browserLockdown;
    }

    public Integer getQuestionsCount() {
        return questionsCount;
    }

    public void setQuestionsCount(Integer questionsCount) {
        this.questionsCount = questionsCount;
    }

    public Integer getAttemptsCount() {
        return attemptsCount;
    }

    public void setAttemptsCount(Integer attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public Integer getCompletedAttempts() {
        return completedAttempts;
    }

    public void setCompletedAttempts(Integer completedAttempts) {
        this.completedAttempts = completedAttempts;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
    }

    public String getAvgDuration() {
        return avgDuration;
    }

    public void setAvgDuration(String avgDuration) {
        this.avgDuration = avgDuration;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getFirstAttemptAt() {
        return firstAttemptAt;
    }

    public void setFirstAttemptAt(LocalDateTime firstAttemptAt) {
        this.firstAttemptAt = firstAttemptAt;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ExamQuestion> getExamQuestions() {
        return examQuestions;
    }

    public void setExamQuestions(List<ExamQuestion> examQuestions) {
        this.examQuestions = examQuestions;
    }

    // 业务方法
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return ExamStatus.ACTIVE.equals(this.status) ||
               (ExamStatus.PUBLISHED.equals(this.status) && 
                (availableFrom == null || now.isAfter(availableFrom)) &&
                (availableUntil == null || now.isBefore(availableUntil)));
    }

    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return (availableFrom == null || now.isAfter(availableFrom)) &&
               (availableUntil == null || now.isBefore(availableUntil));
    }

    public boolean canPublish() {
        return ExamStatus.DRAFT.equals(this.status) || ExamStatus.REVIEW.equals(this.status);
    }

    public void publish() {
        if (canPublish()) {
            this.status = ExamStatus.PUBLISHED;
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void archive() {
        this.status = ExamStatus.ARCHIVED;
    }

    public String getTypeCode() {
        return type != null ? type.getCode() : null;
    }

    public String getTypeDescription() {
        return type != null ? type.getDescription() : null;
    }

    public String getStatusCode() {
        return status != null ? status.getCode() : null;
    }

    public String getStatusDescription() {
        return status != null ? status.getDescription() : null;
    }

    public void updateStatistics() {
        // 更新统计信息的方法，由业务层调用
        this.questionsCount = examQuestions != null ? examQuestions.size() : 0;
    }

    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", code='" + code + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", durationMinutes=" + durationMinutes +
                ", questionsCount=" + questionsCount +
                ", createdAt=" + createdAt +
                '}';
    }
}