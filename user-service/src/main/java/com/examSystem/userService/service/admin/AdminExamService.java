package com.examSystem.userService.service.admin;

import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.entity.ExamQuestion;
import com.examSystem.userService.entity.Question;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.repository.ExamQuestionRepository;
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
 * 管理员考试管理服务
 * 
 * 提供考试的CRUD操作、试卷组卷、发布管理等功能
 */
@Service
@Transactional
public class AdminExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * 创建新考试
     */
    public Exam createExam(Exam exam) {
        // 生成唯一考试代码
        if (exam.getCode() == null || exam.getCode().isEmpty()) {
            exam.setCode(generateUniqueExamCode());
        }
        
        // 设置默认值
        if (exam.getStatus() == null) {
            exam.setStatus(Exam.ExamStatus.DRAFT);
        }
        if (exam.getAttemptsCount() == null) {
            exam.setAttemptsCount(0);
        }
        if (exam.getCompletedAttempts() == null) {
            exam.setCompletedAttempts(0);
        }
        if (exam.getAvgScore() == null) {
            exam.setAvgScore(BigDecimal.ZERO);
        }
        
        return examRepository.save(exam);
    }

    /**
     * 根据ID获取考试
     */
    @Transactional(readOnly = true)
    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    /**
     * 更新考试信息
     */
    public Exam updateExam(Long id, Exam examUpdate) {
        Exam existingExam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + id));

        // 检查考试状态是否允许修改
        if (Exam.ExamStatus.ACTIVE.equals(existingExam.getStatus()) || 
            Exam.ExamStatus.COMPLETED.equals(existingExam.getStatus())) {
            throw new RuntimeException("进行中或已完成的考试不能修改");
        }

        // 更新基本信息
        if (examUpdate.getTitle() != null) {
            existingExam.setTitle(examUpdate.getTitle());
        }
        if (examUpdate.getDescription() != null) {
            existingExam.setDescription(examUpdate.getDescription());
        }
        if (examUpdate.getType() != null) {
            existingExam.setType(examUpdate.getType());
        }
        if (examUpdate.getDurationMinutes() != null) {
            existingExam.setDurationMinutes(examUpdate.getDurationMinutes());
        }
        if (examUpdate.getPassingScore() != null) {
            existingExam.setPassingScore(examUpdate.getPassingScore());
        }
        if (examUpdate.getTotalPoints() != null) {
            existingExam.setTotalPoints(examUpdate.getTotalPoints());
        }
        if (examUpdate.getQuestionsCount() != null) {
            existingExam.setQuestionsCount(examUpdate.getQuestionsCount());
        }
        if (examUpdate.getMaxAttempts() != null) {
            existingExam.setMaxAttempts(examUpdate.getMaxAttempts());
        }
        if (examUpdate.getAvailableFrom() != null) {
            existingExam.setAvailableFrom(examUpdate.getAvailableFrom());
        }
        if (examUpdate.getAvailableUntil() != null) {
            existingExam.setAvailableUntil(examUpdate.getAvailableUntil());
        }
        if (examUpdate.getInstructions() != null) {
            existingExam.setInstructions(examUpdate.getInstructions());
        }
        if (examUpdate.getSettings() != null) {
            existingExam.setSettings(examUpdate.getSettings());
        }

        return examRepository.save(existingExam);
    }

    /**
     * 删除考试（软删除）
     */
    public void deleteExam(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + id));

        // 检查考试状态
        if (Exam.ExamStatus.ACTIVE.equals(exam.getStatus())) {
            throw new RuntimeException("进行中的考试不能删除");
        }

        exam.setStatus(Exam.ExamStatus.DELETED);
        examRepository.save(exam);
    }

    /**
     * 批量删除考试
     */
    public void batchDeleteExams(List<Long> examIds) {
        for (Long id : examIds) {
            deleteExam(id);
        }
    }

    /**
     * 根据状态获取考试列表
     */
    @Transactional(readOnly = true)
    public Page<Exam> getExamsByStatus(Exam.ExamStatus status, Pageable pageable) {
        return examRepository.findByStatus(status, pageable);
    }

    /**
     * 根据创建者获取考试列表
     */
    @Transactional(readOnly = true)
    public Page<Exam> getExamsByCreator(Long createdBy, Pageable pageable) {
        return examRepository.findByCreatedBy(createdBy, pageable);
    }

    /**
     * 根据课程ID获取考试列表
     */
    @Transactional(readOnly = true)
    public Page<Exam> getExamsByCourse(Long courseId, Pageable pageable) {
        return examRepository.findByCourseId(courseId, pageable);
    }

    /**
     * 搜索考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> searchExams(String keyword, Pageable pageable) {
        return examRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 高级搜索考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> advancedSearchExams(Exam.ExamType type,
                                         Exam.ExamStatus status,
                                         Long courseId,
                                         Long createdBy,
                                         String keyword,
                                         Pageable pageable) {
        return examRepository.advancedSearch(type, status, courseId, createdBy, keyword, pageable);
    }

    /**
     * 根据时间范围获取考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> getExamsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return examRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * 获取草稿状态的考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> getDraftExamsByCreator(Long createdBy, Pageable pageable) {
        return examRepository.findDraftExamsByCreator(createdBy, pageable);
    }

    /**
     * 获取等待审核的考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> getExamsForReview(Pageable pageable) {
        return examRepository.findExamsForReview(pageable);
    }

    /**
     * 获取热门考试
     */
    @Transactional(readOnly = true)
    public Page<Exam> getPopularExams(Pageable pageable) {
        return examRepository.findPopularExams(pageable);
    }

    /**
     * 添加题目到考试
     */
    public ExamQuestion addQuestionToExam(Long examId, Long questionId, BigDecimal points, Integer order) {
        // 验证考试存在
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        // 验证题目存在
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));

        // 检查考试状态
        if (!Exam.ExamStatus.DRAFT.equals(exam.getStatus()) && 
            !Exam.ExamStatus.REVIEW.equals(exam.getStatus())) {
            throw new RuntimeException("只有草稿或审核状态的考试才能添加题目");
        }

        // 检查题目是否已存在
        if (examQuestionRepository.existsByExamIdAndQuestionId(examId, questionId)) {
            throw new RuntimeException("题目已存在于考试中");
        }

        // 创建考试题目关联
        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(examId);
        examQuestion.setQuestionId(questionId);
        examQuestion.setPoints(points);
        
        // 设置题目顺序
        if (order == null) {
            Integer maxOrder = examQuestionRepository.findMaxQuestionOrderByExamId(examId);
            examQuestion.setQuestionOrder(maxOrder != null ? maxOrder + 1 : 1);
        } else {
            examQuestion.setQuestionOrder(order);
        }

        examQuestion.setIsRequired(true);

        ExamQuestion saved = examQuestionRepository.save(examQuestion);

        // 更新考试统计信息
        updateExamQuestionStatistics(examId);

        return saved;
    }

    /**
     * 批量添加题目到考试
     */
    public void batchAddQuestionsToExam(Long examId, List<Long> questionIds, BigDecimal defaultPoints) {
        for (Long questionId : questionIds) {
            addQuestionToExam(examId, questionId, defaultPoints, null);
        }
    }

    /**
     * 从考试中移除题目
     */
    public void removeQuestionFromExam(Long examId, Long questionId) {
        // 验证考试存在且状态允许修改
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));
        
        if (!Exam.ExamStatus.DRAFT.equals(exam.getStatus()) && 
            !Exam.ExamStatus.REVIEW.equals(exam.getStatus())) {
            throw new RuntimeException("只有草稿或审核状态的考试才能移除题目");
        }

        // 删除关联关系
        examQuestionRepository.deleteByExamIdAndQuestionId(examId, questionId);

        // 更新考试统计信息
        updateExamQuestionStatistics(examId);
    }

    /**
     * 批量移除考试题目
     */
    public void batchRemoveQuestionsFromExam(Long examId, List<Long> questionIds) {
        examQuestionRepository.batchDeleteByExamIdAndQuestionIds(examId, questionIds);
        updateExamQuestionStatistics(examId);
    }

    /**
     * 获取考试的所有题目
     */
    @Transactional(readOnly = true)
    public List<ExamQuestion> getExamQuestions(Long examId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrder(examId);
    }

    /**
     * 分页获取考试题目
     */
    @Transactional(readOnly = true)
    public Page<ExamQuestion> getExamQuestions(Long examId, Pageable pageable) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrder(examId, pageable);
    }

    /**
     * 更新题目分值
     */
    public void updateQuestionPoints(Long examId, Long questionId, BigDecimal points) {
        examQuestionRepository.updatePoints(examId, questionId, points);
        updateExamQuestionStatistics(examId);
    }

    /**
     * 更新题目顺序
     */
    public void updateQuestionOrder(Long examId, Long questionId, Integer order) {
        examQuestionRepository.updateQuestionOrder(examId, questionId, order);
    }

    /**
     * 重新排序考试题目
     */
    public void reorderExamQuestions(Long examId) {
        examQuestionRepository.reorderQuestions(examId);
    }

    /**
     * 更新考试状态
     */
    public void updateExamStatus(Long examId, Exam.ExamStatus status) {
        examRepository.updateStatus(examId, status);
    }

    /**
     * 发布考试
     */
    public void publishExam(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        // 验证考试是否可以发布
        validateExamForPublish(exam);

        examRepository.publishExam(examId, LocalDateTime.now());
    }

    /**
     * 归档考试
     */
    public void archiveExam(Long examId) {
        examRepository.archiveExam(examId);
    }

    /**
     * 批量更新考试状态
     */
    public void batchUpdateExamStatus(List<Long> examIds, Exam.ExamStatus status) {
        examRepository.batchUpdateStatus(examIds, status);
    }

    /**
     * 复制考试
     */
    public Exam copyExam(Long sourceExamId, String newTitle, Long createdBy) {
        Exam sourceExam = examRepository.findById(sourceExamId)
            .orElseThrow(() -> new RuntimeException("源考试不存在: " + sourceExamId));

        // 创建新考试
        Exam newExam = new Exam();
        newExam.setTitle(newTitle);
        newExam.setDescription(sourceExam.getDescription());
        newExam.setType(sourceExam.getType());
        newExam.setDurationMinutes(sourceExam.getDurationMinutes());
        newExam.setPassingScore(sourceExam.getPassingScore());
        newExam.setMaxAttempts(sourceExam.getMaxAttempts());
        newExam.setInstructions(sourceExam.getInstructions());
        newExam.setSettings(sourceExam.getSettings());
        newExam.setCreatedBy(createdBy);
        newExam.setCode(generateUniqueExamCode());
        newExam.setStatus(Exam.ExamStatus.DRAFT);
        
        // 保存新考试
        Exam savedExam = examRepository.save(newExam);

        // 复制题目
        List<ExamQuestion> sourceQuestions = examQuestionRepository.findQuestionsForCopy(sourceExamId);
        for (ExamQuestion sourceQuestion : sourceQuestions) {
            ExamQuestion newQuestion = new ExamQuestion();
            newQuestion.setExamId(savedExam.getId());
            newQuestion.setQuestionId(sourceQuestion.getQuestionId());
            newQuestion.setQuestionOrder(sourceQuestion.getQuestionOrder());
            newQuestion.setPoints(sourceQuestion.getPoints());
            newQuestion.setIsRequired(sourceQuestion.getIsRequired());
            newQuestion.setSectionId(sourceQuestion.getSectionId());
            newQuestion.setSectionName(sourceQuestion.getSectionName());
            newQuestion.setTimeLimitSeconds(sourceQuestion.getTimeLimitSeconds());
            
            examQuestionRepository.save(newQuestion);
        }

        // 更新新考试的统计信息
        updateExamQuestionStatistics(savedExam.getId());

        return savedExam;
    }

    /**
     * 获取考试统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExamStatistics() {
        return examRepository.getExamStatistics();
    }

    /**
     * 按类型统计考试数量
     */
    @Transactional(readOnly = true)
    public List<Object[]> countExamsByType() {
        return examRepository.countExamsByType();
    }

    /**
     * 按状态统计考试数量
     */
    @Transactional(readOnly = true)
    public List<Object[]> countExamsByStatus() {
        return examRepository.countExamsByStatus();
    }

    /**
     * 统计指定创建者的考试数量
     */
    @Transactional(readOnly = true)
    public long countExamsByCreator(Long createdBy) {
        return examRepository.countByCreatedBy(createdBy);
    }

    /**
     * 获取考试参与统计
     */
    @Transactional(readOnly = true)
    public Page<Object[]> getExamParticipationStatistics(Pageable pageable) {
        return examRepository.getExamParticipationStatistics(pageable);
    }

    /**
     * 获取指定时间段的考试统计
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExamCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return examRepository.getExamCountByDateRange(startDate, endDate);
    }

    /**
     * 获取考试难度分布
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExamDifficultyDistribution() {
        return examRepository.getExamDifficultyDistribution();
    }

    // 私有辅助方法

    /**
     * 生成唯一的考试代码
     */
    private String generateUniqueExamCode() {
        String code;
        do {
            code = "EXAM_" + System.currentTimeMillis() + "_" + 
                   UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (examRepository.existsByCode(code));
        return code;
    }

    /**
     * 更新考试题目统计信息
     */
    private void updateExamQuestionStatistics(Long examId) {
        List<Object[]> stats = examQuestionRepository.getExamQuestionStatistics(examId);
        if (!stats.isEmpty()) {
            Object[] stat = stats.get(0);
            Integer totalQuestions = stat[0] != null ? ((Number) stat[0]).intValue() : 0;
            BigDecimal totalPoints = stat[1] != null ? (BigDecimal) stat[1] : BigDecimal.ZERO;
            
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam != null) {
                exam.setQuestionsCount(totalQuestions);
                exam.setTotalPoints(totalPoints);
                examRepository.save(exam);
            }
        }
    }

    /**
     * 验证考试是否可以发布
     */
    private void validateExamForPublish(Exam exam) {
        if (exam.getQuestionsCount() == null || exam.getQuestionsCount() == 0) {
            throw new RuntimeException("考试必须包含至少一道题目才能发布");
        }
        
        if (exam.getTotalPoints() == null || exam.getTotalPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("考试总分必须大于0才能发布");
        }
        
        if (exam.getDurationMinutes() == null || exam.getDurationMinutes() <= 0) {
            throw new RuntimeException("考试时长必须设置才能发布");
        }
        
        if (exam.getPassingScore() == null) {
            throw new RuntimeException("及格分数必须设置才能发布");
        }
    }
}