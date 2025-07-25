package com.examSystem.userService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试题目关联实体类
 * 
 * 基于数据库设计文档中的exam_questions表结构
 * 管理考试和题目的多对多关系，支持题目排序和分值设置
 */
@Entity
@Table(name = "exam_questions", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_exam_question", 
                           columnNames = {"exam_id", "question_id"})
       },
       indexes = {
           @Index(name = "idx_exam_questions_exam_id", columnList = "exam_id"),
           @Index(name = "idx_exam_questions_question_id", columnList = "question_id"),
           @Index(name = "idx_exam_questions_order", columnList = "question_order"),
           @Index(name = "idx_exam_questions_section", columnList = "section_id"),
           @Index(name = "idx_exam_questions_is_required", columnList = "is_required")
       })
@EntityListeners(AuditingEntityListener.class)
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    @NotNull(message = "考试ID不能为空")
    private Long examId;

    @Column(name = "question_id", nullable = false)
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @Column(name = "question_order", nullable = false)
    @NotNull(message = "题目顺序不能为空")
    @Min(value = 1, message = "题目顺序必须大于0")
    private Integer questionOrder;

    @Column(name = "points", precision = 6, scale = 2, nullable = false)
    @NotNull(message = "题目分值不能为空")
    @DecimalMin(value = "0.01", message = "题目分值必须大于0")
    private BigDecimal points;

    @Column(name = "section_id")
    private Long sectionId; // 可选的章节ID，用于分组

    @Column(name = "section_name", length = 100)
    private String sectionName; // 章节名称

    @Column(name = "time_limit_seconds")
    @Min(value = 0, message = "时间限制不能为负")
    private Integer timeLimitSeconds; // 单题时间限制

    @Column(name = "is_required")
    private Boolean isRequired = true; // 是否必答题

    @Column(name = "partial_credit_enabled")
    private Boolean partialCreditEnabled = false; // 是否启用部分得分

    @Column(name = "negative_marking_enabled")
    private Boolean negativeMarkingEnabled = false; // 是否启用负分标记

    @Column(name = "question_settings", columnDefinition = "jsonb")
    private String questionSettings = "{}"; // 题目特定设置

    @Column(name = "display_settings", columnDefinition = "jsonb")
    private String displaySettings = "{}"; // 显示设置

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}"; // 元数据

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", insertable = false, updatable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Question question;

    // 默认构造函数
    public ExamQuestion() {}

    // 构造函数
    public ExamQuestion(Long examId, Long questionId, Integer questionOrder, BigDecimal points) {
        this.examId = examId;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.points = points;
    }

    public ExamQuestion(Long examId, Long questionId, Integer questionOrder, BigDecimal points, String sectionName) {
        this.examId = examId;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.points = points;
        this.sectionName = sectionName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getPartialCreditEnabled() {
        return partialCreditEnabled;
    }

    public void setPartialCreditEnabled(Boolean partialCreditEnabled) {
        this.partialCreditEnabled = partialCreditEnabled;
    }

    public Boolean getNegativeMarkingEnabled() {
        return negativeMarkingEnabled;
    }

    public void setNegativeMarkingEnabled(Boolean negativeMarkingEnabled) {
        this.negativeMarkingEnabled = negativeMarkingEnabled;
    }

    public String getQuestionSettings() {
        return questionSettings;
    }

    public void setQuestionSettings(String questionSettings) {
        this.questionSettings = questionSettings;
    }

    public String getDisplaySettings() {
        return displaySettings;
    }

    public void setDisplaySettings(String displaySettings) {
        this.displaySettings = displaySettings;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    // 业务方法
    public boolean hasTimeLimit() {
        return timeLimitSeconds != null && timeLimitSeconds > 0;
    }

    public boolean isOptional() {
        return !Boolean.TRUE.equals(isRequired);
    }

    public boolean hasSection() {
        return sectionId != null || (sectionName != null && !sectionName.trim().isEmpty());
    }

    public String getEffectiveSectionName() {
        return sectionName != null && !sectionName.trim().isEmpty() ? sectionName : "默认章节";
    }

    @Override
    public String toString() {
        return "ExamQuestion{" +
                "id=" + id +
                ", examId=" + examId +
                ", questionId=" + questionId +
                ", questionOrder=" + questionOrder +
                ", points=" + points +
                ", sectionName='" + sectionName + '\'' +
                ", isRequired=" + isRequired +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ExamQuestion that = (ExamQuestion) o;
        
        if (examId != null ? !examId.equals(that.examId) : that.examId != null) return false;
        return questionId != null ? questionId.equals(that.questionId) : that.questionId == null;
    }

    @Override
    public int hashCode() {
        int result = examId != null ? examId.hashCode() : 0;
        result = 31 * result + (questionId != null ? questionId.hashCode() : 0);
        return result;
    }
}