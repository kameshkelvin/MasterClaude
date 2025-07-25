package com.examSystem.userService.repository;

import com.examSystem.userService.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 题目数据访问接口
 * 
 * 基于数据库设计文档中的questions表结构
 * 提供题目相关的数据库操作方法，支持全文搜索和统计查询
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 根据题目类型查找题目
     */
    Page<Question> findByTypeAndStatus(Question.QuestionType type, Question.QuestionStatus status, Pageable pageable);

    /**
     * 根据难度级别查找题目
     */
    Page<Question> findByDifficultyAndStatus(Question.DifficultyLevel difficulty, Question.QuestionStatus status, Pageable pageable);

    /**
     * 根据创建者查找题目
     */
    Page<Question> findByCreatedByAndStatus(Long createdBy, Question.QuestionStatus status, Pageable pageable);

    /**
     * 根据组织ID查找题目
     */
    Page<Question> findByOrganizationIdAndStatus(Long organizationId, Question.QuestionStatus status, Pageable pageable);

    /**
     * 根据分类ID查找题目
     */
    Page<Question> findByCategoryIdAndStatus(Long categoryId, Question.QuestionStatus status, Pageable pageable);

    /**
     * 根据状态查找题目
     */
    Page<Question> findByStatus(Question.QuestionStatus status, Pageable pageable);

    /**
     * 查找活跃的题目
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'ACTIVE' AND q.isLatestVersion = true")
    Page<Question> findActiveQuestions(Pageable pageable);

    /**
     * 根据关键字搜索题目（全文搜索）
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'ACTIVE' AND " +
           "(LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.explanation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.topic) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 高级搜索：根据多个条件搜索题目
     */
    @Query("SELECT q FROM Question q WHERE " +
           "(:type IS NULL OR q.type = :type) AND " +
           "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
           "(:categoryId IS NULL OR q.categoryId = :categoryId) AND " +
           "(:organizationId IS NULL OR q.organizationId = :organizationId) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:keyword IS NULL OR " +
           "  LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(q.subject) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> advancedSearch(@Param("type") Question.QuestionType type,
                                @Param("difficulty") Question.DifficultyLevel difficulty,
                                @Param("categoryId") Long categoryId,
                                @Param("organizationId") Long organizationId,
                                @Param("status") Question.QuestionStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    /**
     * 根据科目和主题查找题目
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'ACTIVE' AND " +
           "(:subject IS NULL OR LOWER(q.subject) = LOWER(:subject)) AND " +
           "(:topic IS NULL OR LOWER(q.topic) = LOWER(:topic))")
    Page<Question> findBySubjectAndTopic(@Param("subject") String subject, 
                                       @Param("topic") String topic, 
                                       Pageable pageable);

    /**
     * 随机获取指定数量的题目
     */
    @Query(value = "SELECT * FROM questions WHERE status = 'ACTIVE' AND " +
                   "(:type IS NULL OR type = CAST(:type AS varchar)) AND " +
                   "(:difficulty IS NULL OR difficulty = CAST(:difficulty AS varchar)) " +
                   "ORDER BY RANDOM() LIMIT :limit", 
           nativeQuery = true)
    List<Question> findRandomQuestions(@Param("type") String type,
                                     @Param("difficulty") String difficulty,
                                     @Param("limit") int limit);

    /**
     * 获取热门题目（按使用频率排序）
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'ACTIVE' AND q.usageCount > 0 " +
           "ORDER BY q.usageCount DESC, q.lastUsedAt DESC")
    Page<Question> findPopularQuestions(Pageable pageable);

    /**
     * 获取需要审核的题目
     */
    @Query("SELECT q FROM Question q WHERE q.reviewStatus = 'pending' OR q.reviewStatus = 'revision_required' " +
           "ORDER BY q.createdAt ASC")
    Page<Question> findQuestionsNeedingReview(Pageable pageable);

    /**
     * 根据创建者和审核状态查找题目
     */
    Page<Question> findByCreatedByAndReviewStatus(Long createdBy, String reviewStatus, Pageable pageable);

    /**
     * 统计各种类型的题目数量
     */
    @Query("SELECT q.type, COUNT(q) FROM Question q WHERE q.status = 'ACTIVE' GROUP BY q.type")
    List<Object[]> countQuestionsByType();

    /**
     * 统计各难度级别的题目数量
     */
    @Query("SELECT q.difficulty, COUNT(q) FROM Question q WHERE q.status = 'ACTIVE' GROUP BY q.difficulty")
    List<Object[]> countQuestionsByDifficulty();

    /**
     * 统计指定组织的题目数量
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.organizationId = :organizationId AND q.status = 'ACTIVE'")
    long countByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * 统计指定创建者的题目数量
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.createdBy = :createdBy AND q.status = 'ACTIVE'")
    long countByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 获取题目的统计信息
     */
    @Query("SELECT COUNT(q), AVG(q.usageCount), AVG(q.correctRate), AVG(q.avgScore) " +
           "FROM Question q WHERE q.status = 'ACTIVE'")
    List<Object[]> getQuestionStatistics();

    /**
     * 更新题目使用统计
     */
    @Modifying
    @Query("UPDATE Question q SET q.usageCount = q.usageCount + 1, q.lastUsedAt = :usedAt WHERE q.id = :questionId")
    void updateUsageStatistics(@Param("questionId") Long questionId, @Param("usedAt") LocalDateTime usedAt);

    /**
     * 批量更新题目统计信息
     */
    @Modifying
    @Query("UPDATE Question q SET " +
           "q.correctRate = :correctRate, " +
           "q.avgScore = :avgScore, " +
           "q.avgTimeSeconds = :avgTimeSeconds, " +
           "q.difficultyIndex = :difficultyIndex " +
           "WHERE q.id = :questionId")
    void updateQuestionStatistics(@Param("questionId") Long questionId,
                                @Param("correctRate") BigDecimal correctRate,
                                @Param("avgScore") BigDecimal avgScore,
                                @Param("avgTimeSeconds") Integer avgTimeSeconds,
                                @Param("difficultyIndex") BigDecimal difficultyIndex);

    /**
     * 更新题目审核状态
     */
    @Modifying
    @Query("UPDATE Question q SET " +
           "q.reviewStatus = :reviewStatus, " +
           "q.reviewedBy = :reviewedBy, " +
           "q.reviewedAt = :reviewedAt, " +
           "q.reviewNotes = :reviewNotes " +
           "WHERE q.id = :questionId")
    void updateReviewStatus(@Param("questionId") Long questionId,
                          @Param("reviewStatus") String reviewStatus,
                          @Param("reviewedBy") Long reviewedBy,
                          @Param("reviewedAt") LocalDateTime reviewedAt,
                          @Param("reviewNotes") String reviewNotes);

    /**
     * 更新题目状态
     */
    @Modifying
    @Query("UPDATE Question q SET q.status = :status WHERE q.id = :questionId")
    void updateStatus(@Param("questionId") Long questionId, @Param("status") Question.QuestionStatus status);

    /**
     * 批量更新题目状态
     */
    @Modifying
    @Query("UPDATE Question q SET q.status = :status WHERE q.id IN :questionIds")
    void batchUpdateStatus(@Param("questionIds") List<Long> questionIds, @Param("status") Question.QuestionStatus status);

    /**
     * 创建新版本题目时，将旧版本标记为非最新
     */
    @Modifying
    @Query("UPDATE Question q SET q.isLatestVersion = false WHERE q.parentQuestionId = :parentQuestionId AND q.id != :newVersionId")
    void markOldVersionsAsNotLatest(@Param("parentQuestionId") Long parentQuestionId, @Param("newVersionId") Long newVersionId);

    /**
     * 查找题目的所有版本
     */
    @Query("SELECT q FROM Question q WHERE " +
           "(q.id = :questionId OR q.parentQuestionId = :questionId OR " +
           "(q.parentQuestionId IS NOT NULL AND q.parentQuestionId IN " +
           "  (SELECT q2.parentQuestionId FROM Question q2 WHERE q2.id = :questionId))) " +
           "ORDER BY q.version DESC")
    List<Question> findAllVersions(@Param("questionId") Long questionId);

    /**
     * 查找题目的最新版本
     */
    @Query("SELECT q FROM Question q WHERE " +
           "(q.id = :questionId OR q.parentQuestionId = :questionId OR " +
           "(q.parentQuestionId IS NOT NULL AND q.parentQuestionId IN " +
           "  (SELECT q2.parentQuestionId FROM Question q2 WHERE q2.id = :questionId))) " +
           "AND q.isLatestVersion = true")
    Optional<Question> findLatestVersion(@Param("questionId") Long questionId);

    /**
     * 检查题目是否被考试使用
     */
    @Query("SELECT COUNT(eq) > 0 FROM ExamQuestion eq WHERE eq.questionId = :questionId")
    boolean isQuestionInUse(@Param("questionId") Long questionId);

    /**
     * 查找相似题目（基于内容相似度）
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'ACTIVE' AND q.id != :questionId AND " +
           "q.type = :type AND q.subject = :subject AND " +
           "(LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Question> findSimilarQuestions(@Param("questionId") Long questionId,
                                      @Param("type") Question.QuestionType type,
                                      @Param("subject") String subject,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    /**
     * 获取最近创建的题目
     */
    @Query("SELECT q FROM Question q WHERE q.createdBy = :createdBy " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findRecentQuestionsByCreator(@Param("createdBy") Long createdBy, Pageable pageable);

    /**
     * 根据标签查找题目
     */
    @Query(value = "SELECT * FROM questions WHERE status = 'ACTIVE' AND " +
                   "tags::jsonb ? :tag", nativeQuery = true)
    Page<Question> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 获取题目使用情况统计
     */
    @Query("SELECT q.id, q.title, q.usageCount, q.correctRate, q.lastUsedAt " +
           "FROM Question q WHERE q.status = 'ACTIVE' AND q.usageCount > 0 " +
           "ORDER BY q.usageCount DESC")
    Page<Object[]> getQuestionUsageStatistics(Pageable pageable);
}