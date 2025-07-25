package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目实体类
 * 
 * 基于数据库设计文档中的questions表结构
 * 支持多种题型和全文搜索功能
 */
@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_questions_type", columnList = "type"),
    @Index(name = "idx_questions_difficulty", columnList = "difficulty"),
    @Index(name = "idx_questions_status", columnList = "status"),
    @Index(name = "idx_questions_category", columnList = "category_id"),
    @Index(name = "idx_questions_created_by", columnList = "created_by"),
    @Index(name = "idx_questions_organization", columnList = "organization_id"),
    @Index(name = "idx_questions_usage_count", columnList = "usage_count"),
    @Index(name = "idx_questions_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    @NotBlank(message = "题目内容不能为空")
    private String content;

    @Column(name = "type", nullable = false, length = 20)
    @NotBlank(message = "题目类型不能为空")
    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @Column(name = "difficulty", length = 20)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Column(name = "default_points", precision = 6, scale = 2)
    @NotNull(message = "题目分值不能为空")
    @DecimalMin(value = "0.01", message = "题目分值必须大于0")
    private BigDecimal defaultPoints = BigDecimal.ONE;

    @Column(name = "partial_credit")
    private Boolean partialCredit = false;

    @Column(name = "negative_marking")
    private Boolean negativeMarking = false;

    @Column(name = "options", columnDefinition = "jsonb")
    private String options; // JSON格式存储选择题选项

    @Column(name = "correct_answer", columnDefinition = "jsonb")
    @NotBlank(message = "正确答案不能为空")
    private String correctAnswer; // JSON格式存储正确答案

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation; // 答案解析

    @Column(name = "hints", columnDefinition = "jsonb")
    private String hints = "[]"; // JSON格式存储提示数组

    @Column(name = "media_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "media_metadata", columnDefinition = "jsonb")
    private String mediaMetadata = "{}";

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "topic", length = 100)
    private String topic;

    @Column(name = "subtopic", length = 100)
    private String subtopic;

    @Column(name = "keywords", columnDefinition = "text[]")
    private String keywords; // PostgreSQL数组类型

    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags = "[]";

    @Column(name = "created_by", nullable = false)
    @NotNull(message = "创建者ID不能为空")
    private Long createdBy;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "copyright_info", columnDefinition = "TEXT")
    private String copyrightInfo;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private QuestionStatus status = QuestionStatus.DRAFT;

    @Column(name = "review_status", length = 20)
    private String reviewStatus = "pending";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    // 统计字段
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "correct_rate", precision = 5, scale = 2)
    @DecimalMin(value = "0", message = "正确率不能为负")
    @DecimalMax(value = "100", message = "正确率不能超过100")
    private BigDecimal correctRate;

    @Column(name = "avg_score", precision = 5, scale = 2)
    private BigDecimal avgScore;

    @Column(name = "avg_time_seconds")
    private Integer avgTimeSeconds;

    @Column(name = "difficulty_index", precision = 3, scale = 2)
    @DecimalMin(value = "0", message = "难度系数不能为负")
    @DecimalMax(value = "1", message = "难度系数不能超过1")
    private BigDecimal difficultyIndex;

    @Column(name = "discrimination_index", precision = 3, scale = 2)
    @DecimalMin(value = "0", message = "区分度不能为负")
    @DecimalMax(value = "1", message = "区分度不能超过1")
    private BigDecimal discriminationIndex;

    // 版本控制
    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "parent_question_id")
    private Long parentQuestionId;

    @Column(name = "is_latest_version")
    private Boolean isLatestVersion = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 枚举定义
    public enum QuestionType {
        SINGLE_CHOICE("single_choice", "单选题"),
        MULTIPLE_CHOICE("multiple_choice", "多选题"),
        TRUE_FALSE("true_false", "判断题"),
        FILL_BLANK("fill_blank", "填空题"),
        SHORT_ANSWER("short_answer", "简答题"),
        ESSAY("essay", "论述题"),
        CODING("coding", "编程题"),
        MATCHING("matching", "匹配题"),
        ORDERING("ordering", "排序题"),
        HOTSPOT("hotspot", "热点题");

        private final String code;
        private final String description;

        QuestionType(String code, String description) {
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

    public enum DifficultyLevel {
        EASY("easy", "简单"),
        MEDIUM("medium", "中等"),
        HARD("hard", "困难"),
        EXPERT("expert", "专家");

        private final String code;
        private final String description;

        DifficultyLevel(String code, String description) {
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

    public enum QuestionStatus {
        DRAFT("draft", "草稿"),
        REVIEW("review", "审核中"),
        ACTIVE("active", "已发布"),
        DEPRECATED("deprecated", "已弃用"),
        ARCHIVED("archived", "已归档");

        private final String code;
        private final String description;

        QuestionStatus(String code, String description) {
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

    public enum MediaType {
        IMAGE("image", "图片"),
        AUDIO("audio", "音频"),
        VIDEO("video", "视频"),
        DOCUMENT("document", "文档");

        private final String code;
        private final String description;

        MediaType(String code, String description) {
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
    public Question() {}

    // 构造函数
    public Question(String content, QuestionType type, String correctAnswer, Long createdBy) {
        this.content = content;
        this.type = type;
        this.correctAnswer = correctAnswer;
        this.createdBy = createdBy;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public BigDecimal getDefaultPoints() {
        return defaultPoints;
    }

    public void setDefaultPoints(BigDecimal defaultPoints) {
        this.defaultPoints = defaultPoints;
    }

    public Boolean getPartialCredit() {
        return partialCredit;
    }

    public void setPartialCredit(Boolean partialCredit) {
        this.partialCredit = partialCredit;
    }

    public Boolean getNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(Boolean negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaMetadata() {
        return mediaMetadata;
    }

    public void setMediaMetadata(String mediaMetadata) {
        this.mediaMetadata = mediaMetadata;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubtopic() {
        return subtopic;
    }

    public void setSubtopic(String subtopic) {
        this.subtopic = subtopic;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCopyrightInfo() {
        return copyrightInfo;
    }

    public void setCopyrightInfo(String copyrightInfo) {
        this.copyrightInfo = copyrightInfo;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public BigDecimal getCorrectRate() {
        return correctRate;
    }

    public void setCorrectRate(BigDecimal correctRate) {
        this.correctRate = correctRate;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
    }

    public Integer getAvgTimeSeconds() {
        return avgTimeSeconds;
    }

    public void setAvgTimeSeconds(Integer avgTimeSeconds) {
        this.avgTimeSeconds = avgTimeSeconds;
    }

    public BigDecimal getDifficultyIndex() {
        return difficultyIndex;
    }

    public void setDifficultyIndex(BigDecimal difficultyIndex) {
        this.difficultyIndex = difficultyIndex;
    }

    public BigDecimal getDiscriminationIndex() {
        return discriminationIndex;
    }

    public void setDiscriminationIndex(BigDecimal discriminationIndex) {
        this.discriminationIndex = discriminationIndex;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getParentQuestionId() {
        return parentQuestionId;
    }

    public void setParentQuestionId(Long parentQuestionId) {
        this.parentQuestionId = parentQuestionId;
    }

    public Boolean getIsLatestVersion() {
        return isLatestVersion;
    }

    public void setIsLatestVersion(Boolean isLatestVersion) {
        this.isLatestVersion = isLatestVersion;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
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

    // 业务方法
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return QuestionStatus.ACTIVE.equals(this.status);
    }

    public boolean needsReview() {
        return "pending".equals(this.reviewStatus) || "revision_required".equals(this.reviewStatus);
    }

    public String getTypeCode() {
        return type != null ? type.getCode() : null;
    }

    public String getTypeDescription() {
        return type != null ? type.getDescription() : null;
    }

    public String getDifficultyCode() {
        return difficulty != null ? difficulty.getCode() : null;
    }

    public String getDifficultyDescription() {
        return difficulty != null ? difficulty.getDescription() : null;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", difficulty=" + difficulty +
                ", status=" + status +
                ", createdBy=" + createdBy +
                ", usageCount=" + usageCount +
                ", createdAt=" + createdAt +
                '}';
    }
}