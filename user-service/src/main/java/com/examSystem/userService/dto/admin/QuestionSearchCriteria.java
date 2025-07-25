package com.examSystem.userService.dto.admin;

import com.examSystem.userService.entity.Question;

/**
 * 题目搜索条件DTO
 */
public class QuestionSearchCriteria {
    
    private String keyword;
    private Question.QuestionType type;
    private Question.DifficultyLevel difficulty;
    private Question.QuestionStatus status;
    private Long categoryId;
    private Long organizationId;
    private Long createdBy;
    private String subject;
    private String topic;
    private String tag;
    private String reviewStatus;

    // 默认构造函数
    public QuestionSearchCriteria() {}

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Question.QuestionType getType() {
        return type;
    }

    public void setType(Question.QuestionType type) {
        this.type = type;
    }

    public Question.DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Question.DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public Question.QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(Question.QuestionStatus status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
}