package com.examSystem.userService.service.admin;

import com.examSystem.userService.entity.Question;
import com.examSystem.userService.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 管理员题库管理服务
 * 
 * 提供题目的CRUD操作和批量管理功能
 * 支持题目审核、统计分析和搜索功能
 */
@Service
@Transactional
public class AdminQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * 创建新题目
     */
    public Question createQuestion(Question question) {
        // 设置默认值
        if (question.getStatus() == null) {
            question.setStatus(Question.QuestionStatus.DRAFT);
        }
        if (question.getReviewStatus() == null) {
            question.setReviewStatus("pending");
        }
        
        question.setIsLatestVersion(true);
        question.setVersion(1);
        question.setUsageCount(0);
        question.setCorrectRate(BigDecimal.ZERO);
        question.setAvgScore(BigDecimal.ZERO);
        question.setDifficultyIndex(BigDecimal.ZERO);
        
        return questionRepository.save(question);
    }

    /**
     * 根据ID获取题目
     */
    @Transactional(readOnly = true)
    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    /**
     * 更新题目信息
     */
    public Question updateQuestion(Long id, Question questionUpdate) {
        Question existingQuestion = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + id));

        // 更新基本信息
        if (questionUpdate.getTitle() != null) {
            existingQuestion.setTitle(questionUpdate.getTitle());
        }
        if (questionUpdate.getContent() != null) {
            existingQuestion.setContent(questionUpdate.getContent());
        }
        if (questionUpdate.getType() != null) {
            existingQuestion.setType(questionUpdate.getType());
        }
        if (questionUpdate.getDifficulty() != null) {
            existingQuestion.setDifficulty(questionUpdate.getDifficulty());
        }
        if (questionUpdate.getOptions() != null) {
            existingQuestion.setOptions(questionUpdate.getOptions());
        }
        if (questionUpdate.getCorrectAnswers() != null) {
            existingQuestion.setCorrectAnswers(questionUpdate.getCorrectAnswers());
        }
        if (questionUpdate.getExplanation() != null) {
            existingQuestion.setExplanation(questionUpdate.getExplanation());
        }
        if (questionUpdate.getSubject() != null) {
            existingQuestion.setSubject(questionUpdate.getSubject());
        }
        if (questionUpdate.getTopic() != null) {
            existingQuestion.setTopic(questionUpdate.getTopic());
        }
        if (questionUpdate.getCategoryId() != null) {
            existingQuestion.setCategoryId(questionUpdate.getCategoryId());
        }
        if (questionUpdate.getTags() != null) {
            existingQuestion.setTags(questionUpdate.getTags());
        }
        if (questionUpdate.getHints() != null) {
            existingQuestion.setHints(questionUpdate.getHints());
        }

        return questionRepository.save(existingQuestion);
    }

    /**
     * 删除题目（软删除）
     */
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + id));

        // 检查题目是否被考试使用
        if (questionRepository.isQuestionInUse(id)) {
            throw new RuntimeException("题目正在被考试使用，无法删除");
        }

        question.setStatus(Question.QuestionStatus.DELETED);
        questionRepository.save(question);
    }

    /**
     * 批量删除题目
     */
    public void batchDeleteQuestions(List<Long> questionIds) {
        for (Long id : questionIds) {
            deleteQuestion(id);
        }
    }

    /**
     * 获取活跃题目列表
     */
    @Transactional(readOnly = true)
    public Page<Question> getActiveQuestions(Pageable pageable) {
        return questionRepository.findActiveQuestions(pageable);
    }

    /**
     * 根据状态获取题目列表
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsByStatus(Question.QuestionStatus status, Pageable pageable) {
        return questionRepository.findByStatus(status, pageable);
    }

    /**
     * 根据类型和状态获取题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsByTypeAndStatus(Question.QuestionType type, 
                                                     Question.QuestionStatus status, 
                                                     Pageable pageable) {
        return questionRepository.findByTypeAndStatus(type, status, pageable);
    }

    /**
     * 根据难度和状态获取题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsByDifficultyAndStatus(Question.DifficultyLevel difficulty, 
                                                           Question.QuestionStatus status, 
                                                           Pageable pageable) {
        return questionRepository.findByDifficultyAndStatus(difficulty, status, pageable);
    }

    /**
     * 根据创建者获取题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsByCreator(Long createdBy, Pageable pageable) {
        return questionRepository.findByCreatedByAndStatus(createdBy, Question.QuestionStatus.ACTIVE, pageable);
    }

    /**
     * 搜索题目
     */
    @Transactional(readOnly = true)
    public Page<Question> searchQuestions(String keyword, Pageable pageable) {
        return questionRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 高级搜索题目
     */
    @Transactional(readOnly = true)
    public Page<Question> advancedSearchQuestions(Question.QuestionType type,
                                                 Question.DifficultyLevel difficulty,
                                                 Long categoryId,
                                                 Long organizationId,
                                                 Question.QuestionStatus status,
                                                 String keyword,
                                                 Pageable pageable) {
        return questionRepository.advancedSearch(type, difficulty, categoryId, organizationId, status, keyword, pageable);
    }

    /**
     * 根据科目和主题搜索题目
     */
    @Transactional(readOnly = true)
    public Page<Question> searchBySubjectAndTopic(String subject, String topic, Pageable pageable) {
        return questionRepository.findBySubjectAndTopic(subject, topic, pageable);
    }

    /**
     * 获取随机题目
     */
    @Transactional(readOnly = true)
    public List<Question> getRandomQuestions(Question.QuestionType type, 
                                           Question.DifficultyLevel difficulty, 
                                           int limit) {
        String typeStr = type != null ? type.name() : null;
        String difficultyStr = difficulty != null ? difficulty.name() : null;
        return questionRepository.findRandomQuestions(typeStr, difficultyStr, limit);
    }

    /**
     * 获取热门题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getPopularQuestions(Pageable pageable) {
        return questionRepository.findPopularQuestions(pageable);
    }

    /**
     * 获取需要审核的题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsNeedingReview(Pageable pageable) {
        return questionRepository.findQuestionsNeedingReview(pageable);
    }

    /**
     * 审核题目
     */
    public void reviewQuestion(Long questionId, String reviewStatus, Long reviewedBy, String reviewNotes) {
        questionRepository.updateReviewStatus(questionId, reviewStatus, reviewedBy, LocalDateTime.now(), reviewNotes);
        
        // 如果审核通过，将状态设为活跃
        if ("approved".equals(reviewStatus)) {
            questionRepository.updateStatus(questionId, Question.QuestionStatus.ACTIVE);
        }
    }

    /**
     * 批量审核题目
     */
    public void batchReviewQuestions(List<Long> questionIds, String reviewStatus, Long reviewedBy, String reviewNotes) {
        LocalDateTime reviewedAt = LocalDateTime.now();
        for (Long questionId : questionIds) {
            questionRepository.updateReviewStatus(questionId, reviewStatus, reviewedBy, reviewedAt, reviewNotes);
            
            if ("approved".equals(reviewStatus)) {
                questionRepository.updateStatus(questionId, Question.QuestionStatus.ACTIVE);
            }
        }
    }

    /**
     * 更新题目状态
     */
    public void updateQuestionStatus(Long questionId, Question.QuestionStatus status) {
        questionRepository.updateStatus(questionId, status);
    }

    /**
     * 批量更新题目状态
     */
    public void batchUpdateQuestionStatus(List<Long> questionIds, Question.QuestionStatus status) {
        questionRepository.batchUpdateStatus(questionIds, status);
    }

    /**
     * 创建题目新版本
     */
    public Question createNewVersion(Long originalQuestionId, Question newVersionData) {
        Question originalQuestion = questionRepository.findById(originalQuestionId)
            .orElseThrow(() -> new RuntimeException("原题目不存在: " + originalQuestionId));

        // 创建新版本
        Question newVersion = new Question();
        
        // 复制基本信息
        newVersion.setTitle(newVersionData.getTitle() != null ? newVersionData.getTitle() : originalQuestion.getTitle());
        newVersion.setContent(newVersionData.getContent() != null ? newVersionData.getContent() : originalQuestion.getContent());
        newVersion.setType(newVersionData.getType() != null ? newVersionData.getType() : originalQuestion.getType());
        newVersion.setDifficulty(newVersionData.getDifficulty() != null ? newVersionData.getDifficulty() : originalQuestion.getDifficulty());
        newVersion.setOptions(newVersionData.getOptions() != null ? newVersionData.getOptions() : originalQuestion.getOptions());
        newVersion.setCorrectAnswers(newVersionData.getCorrectAnswers() != null ? newVersionData.getCorrectAnswers() : originalQuestion.getCorrectAnswers());
        newVersion.setExplanation(newVersionData.getExplanation() != null ? newVersionData.getExplanation() : originalQuestion.getExplanation());
        newVersion.setSubject(originalQuestion.getSubject());
        newVersion.setTopic(originalQuestion.getTopic());
        newVersion.setCategoryId(originalQuestion.getCategoryId());
        newVersion.setOrganizationId(originalQuestion.getOrganizationId());
        newVersion.setCreatedBy(originalQuestion.getCreatedBy());
        
        // 版本信息
        Long parentId = originalQuestion.getParentQuestionId() != null ? 
                       originalQuestion.getParentQuestionId() : originalQuestionId;
        newVersion.setParentQuestionId(parentId);
        newVersion.setVersion(originalQuestion.getVersion() + 1);
        newVersion.setIsLatestVersion(true);
        
        // 状态信息
        newVersion.setStatus(Question.QuestionStatus.DRAFT);
        newVersion.setReviewStatus("pending");
        
        // 统计信息
        newVersion.setUsageCount(0);
        newVersion.setCorrectRate(BigDecimal.ZERO);
        newVersion.setAvgScore(BigDecimal.ZERO);
        newVersion.setDifficultyIndex(BigDecimal.ZERO);

        Question savedNewVersion = questionRepository.save(newVersion);

        // 将旧版本标记为非最新
        questionRepository.markOldVersionsAsNotLatest(parentId, savedNewVersion.getId());

        return savedNewVersion;
    }

    /**
     * 获取题目的所有版本
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionVersions(Long questionId) {
        return questionRepository.findAllVersions(questionId);
    }

    /**
     * 获取题目的最新版本
     */
    @Transactional(readOnly = true)
    public Optional<Question> getLatestVersion(Long questionId) {
        return questionRepository.findLatestVersion(questionId);
    }

    /**
     * 查找相似题目
     */
    @Transactional(readOnly = true)
    public List<Question> findSimilarQuestions(Long questionId, String keyword, Pageable pageable) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        return questionRepository.findSimilarQuestions(questionId, question.getType(), 
                                                      question.getSubject(), keyword, pageable);
    }

    /**
     * 根据标签搜索题目
     */
    @Transactional(readOnly = true)
    public Page<Question> searchByTag(String tag, Pageable pageable) {
        return questionRepository.findByTag(tag, pageable);
    }

    /**
     * 更新题目使用统计
     */
    public void updateUsageStatistics(Long questionId) {
        questionRepository.updateUsageStatistics(questionId, LocalDateTime.now());
    }

    /**
     * 批量更新题目统计信息
     */
    public void updateQuestionStatistics(Long questionId, BigDecimal correctRate, 
                                       BigDecimal avgScore, Integer avgTimeSeconds, 
                                       BigDecimal difficultyIndex) {
        questionRepository.updateQuestionStatistics(questionId, correctRate, avgScore, 
                                                   avgTimeSeconds, difficultyIndex);
    }

    /**
     * 获取题目统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getQuestionStatistics() {
        return questionRepository.getQuestionStatistics();
    }

    /**
     * 按类型统计题目数量
     */
    @Transactional(readOnly = true)
    public List<Object[]> countQuestionsByType() {
        return questionRepository.countQuestionsByType();
    }

    /**
     * 按难度统计题目数量
     */
    @Transactional(readOnly = true)
    public List<Object[]> countQuestionsByDifficulty() {
        return questionRepository.countQuestionsByDifficulty();
    }

    /**
     * 统计指定创建者的题目数量
     */
    @Transactional(readOnly = true)
    public long countQuestionsByCreator(Long createdBy) {
        return questionRepository.countByCreatedBy(createdBy);
    }

    /**
     * 统计指定组织的题目数量
     */
    @Transactional(readOnly = true)
    public long countQuestionsByOrganization(Long organizationId) {
        return questionRepository.countByOrganizationId(organizationId);
    }

    /**
     * 获取题目使用情况统计
     */
    @Transactional(readOnly = true)
    public Page<Object[]> getQuestionUsageStatistics(Pageable pageable) {
        return questionRepository.getQuestionUsageStatistics(pageable);
    }

    /**
     * 获取最近创建的题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getRecentQuestionsByCreator(Long createdBy, Pageable pageable) {
        return questionRepository.findRecentQuestionsByCreator(createdBy, pageable);
    }

    /**
     * 检查题目是否正在使用
     */
    @Transactional(readOnly = true)
    public boolean isQuestionInUse(Long questionId) {
        return questionRepository.isQuestionInUse(questionId);
    }
}