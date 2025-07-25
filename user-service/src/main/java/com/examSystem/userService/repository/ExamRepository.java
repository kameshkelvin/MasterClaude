package com.examSystem.userService.repository;

import com.examSystem.userService.entity.Exam;
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
 * 考试数据访问接口
 * 
 * 基于数据库设计文档中的exams表结构
 * 提供考试相关的数据库操作方法，支持考试管理和统计查询
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * 根据考试代码查找考试
     */
    Optional<Exam> findByCode(String code);

    /**
     * 检查考试代码是否存在
     */
    boolean existsByCode(String code);

    /**
     * 根据课程ID查找考试
     */
    Page<Exam> findByCourseId(Long courseId, Pageable pageable);

    /**
     * 根据创建者查找考试
     */
    Page<Exam> findByCreatedBy(Long createdBy, Pageable pageable);

    /**
     * 根据考试类型查找考试
     */
    Page<Exam> findByType(Exam.ExamType type, Pageable pageable);

    /**
     * 根据状态查找考试
     */
    Page<Exam> findByStatus(Exam.ExamStatus status, Pageable pageable);

    /**
     * 根据创建者和状态查找考试
     */
    Page<Exam> findByCreatedByAndStatus(Long createdBy, Exam.ExamStatus status, Pageable pageable);

    /**
     * 查找已发布的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE') " +
           "ORDER BY e.publishedAt DESC")
    Page<Exam> findPublishedExams(Pageable pageable);

    /**
     * 查找进行中的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'ACTIVE' AND " +
           "(e.availableFrom IS NULL OR e.availableFrom <= :now) AND " +
           "(e.availableUntil IS NULL OR e.availableUntil >= :now)")
    Page<Exam> findActiveExams(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 查找即将开始的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND " +
           "e.availableFrom IS NOT NULL AND e.availableFrom > :now AND " +
           "e.availableFrom <= :upcomingTime")
    List<Exam> findUpcomingExams(@Param("now") LocalDateTime now, 
                               @Param("upcomingTime") LocalDateTime upcomingTime);

    /**
     * 查找已过期的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE') AND " +
           "e.availableUntil IS NOT NULL AND e.availableUntil < :now")
    List<Exam> findExpiredExams(@Param("now") LocalDateTime now);

    /**
     * 根据关键字搜索考试
     */
    @Query("SELECT e FROM Exam e WHERE " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Exam> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 高级搜索：根据多个条件搜索考试
     */
    @Query("SELECT e FROM Exam e WHERE " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:courseId IS NULL OR e.courseId = :courseId) AND " +
           "(:createdBy IS NULL OR e.createdBy = :createdBy) AND " +
           "(:keyword IS NULL OR " +
           "  LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Exam> advancedSearch(@Param("type") Exam.ExamType type,
                            @Param("status") Exam.ExamStatus status,
                            @Param("courseId") Long courseId,
                            @Param("createdBy") Long createdBy,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    /**
     * 根据时间范围查找考试
     */
    @Query("SELECT e FROM Exam e WHERE " +
           "(:startDate IS NULL OR e.availableFrom >= :startDate) AND " +
           "(:endDate IS NULL OR e.availableUntil <= :endDate)")
    Page<Exam> findByDateRange(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             Pageable pageable);

    /**
     * 统计各种类型的考试数量
     */
    @Query("SELECT e.type, COUNT(e) FROM Exam e GROUP BY e.type")
    List<Object[]> countExamsByType();

    /**
     * 统计各状态的考试数量
     */
    @Query("SELECT e.status, COUNT(e) FROM Exam e GROUP BY e.status")
    List<Object[]> countExamsByStatus();

    /**
     * 统计指定创建者的考试数量
     */
    @Query("SELECT COUNT(e) FROM Exam e WHERE e.createdBy = :createdBy")
    long countByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 统计指定课程的考试数量
     */
    @Query("SELECT COUNT(e) FROM Exam e WHERE e.courseId = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    /**
     * 获取考试的基本统计信息
     */
    @Query("SELECT COUNT(e), AVG(e.attemptsCount), AVG(e.avgScore), AVG(e.questionsCount) " +
           "FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE', 'COMPLETED')")
    List<Object[]> getExamStatistics();

    /**
     * 获取最受欢迎的考试（按参与人数排序）
     */
    @Query("SELECT e FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE', 'COMPLETED') AND e.attemptsCount > 0 " +
           "ORDER BY e.attemptsCount DESC")
    Page<Exam> findPopularExams(Pageable pageable);

    /**
     * 获取最近创建的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.createdBy = :createdBy " +
           "ORDER BY e.createdAt DESC")
    Page<Exam> findRecentExamsByCreator(@Param("createdBy") Long createdBy, Pageable pageable);

    /**
     * 更新考试统计信息
     */
    @Modifying
    @Query("UPDATE Exam e SET " +
           "e.attemptsCount = :attemptsCount, " +
           "e.completedAttempts = :completedAttempts, " +
           "e.avgScore = :avgScore, " +
           "e.avgDuration = :avgDuration " +
           "WHERE e.id = :examId")
    void updateExamStatistics(@Param("examId") Long examId,
                            @Param("attemptsCount") Integer attemptsCount,
                            @Param("completedAttempts") Integer completedAttempts,
                            @Param("avgScore") BigDecimal avgScore,
                            @Param("avgDuration") String avgDuration);

    /**
     * 更新考试状态
     */
    @Modifying
    @Query("UPDATE Exam e SET e.status = :status WHERE e.id = :examId")
    void updateStatus(@Param("examId") Long examId, @Param("status") Exam.ExamStatus status);

    /**
     * 发布考试
     */
    @Modifying
    @Query("UPDATE Exam e SET e.status = 'PUBLISHED', e.publishedAt = :publishedAt WHERE e.id = :examId")
    void publishExam(@Param("examId") Long examId, @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * 归档考试
     */
    @Modifying
    @Query("UPDATE Exam e SET e.status = 'ARCHIVED' WHERE e.id = :examId")
    void archiveExam(@Param("examId") Long examId);

    /**
     * 批量更新考试状态
     */
    @Modifying
    @Query("UPDATE Exam e SET e.status = :status WHERE e.id IN :examIds")
    void batchUpdateStatus(@Param("examIds") List<Long> examIds, @Param("status") Exam.ExamStatus status);

    /**
     * 更新考试的第一次和最后一次尝试时间
     */
    @Modifying
    @Query("UPDATE Exam e SET " +
           "e.firstAttemptAt = CASE WHEN e.firstAttemptAt IS NULL THEN :attemptTime ELSE e.firstAttemptAt END, " +
           "e.lastAttemptAt = :attemptTime " +
           "WHERE e.id = :examId")
    void updateAttemptTimes(@Param("examId") Long examId, @Param("attemptTime") LocalDateTime attemptTime);

    /**
     * 增加考试尝试次数
     */
    @Modifying
    @Query("UPDATE Exam e SET e.attemptsCount = e.attemptsCount + 1 WHERE e.id = :examId")
    void incrementAttemptsCount(@Param("examId") Long examId);

    /**
     * 增加考试完成次数
     */
    @Modifying
    @Query("UPDATE Exam e SET e.completedAttempts = e.completedAttempts + 1 WHERE e.id = :examId")
    void incrementCompletedAttempts(@Param("examId") Long examId);

    /**
     * 查找需要自动结束的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'ACTIVE' AND " +
           "e.availableUntil IS NOT NULL AND e.availableUntil <= :now")
    List<Exam> findExamsToAutoEnd(@Param("now") LocalDateTime now);

    /**
     * 查找可以开始的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND " +
           "e.availableFrom IS NOT NULL AND e.availableFrom <= :now")
    List<Exam> findExamsToStart(@Param("now") LocalDateTime now);

    /**
     * 获取考试参与统计
     */
    @Query("SELECT e.id, e.title, e.attemptsCount, e.completedAttempts, e.avgScore " +
           "FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE', 'COMPLETED') " +
           "ORDER BY e.attemptsCount DESC")
    Page<Object[]> getExamParticipationStatistics(Pageable pageable);

    /**
     * 获取指定时间段内的考试统计
     */
    @Query("SELECT DATE(e.createdAt) as examDate, COUNT(e) as examCount " +
           "FROM Exam e WHERE e.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(e.createdAt) ORDER BY examDate")
    List<Object[]> getExamCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 查找草稿状态的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'DRAFT' AND e.createdBy = :createdBy " +
           "ORDER BY e.updatedAt DESC")
    Page<Exam> findDraftExamsByCreator(@Param("createdBy") Long createdBy, Pageable pageable);

    /**
     * 查找等待审核的考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'REVIEW' " +
           "ORDER BY e.updatedAt ASC")
    Page<Exam> findExamsForReview(Pageable pageable);

    /**
     * 统计用户创建的各状态考试数量
     */
    @Query("SELECT e.status, COUNT(e) FROM Exam e WHERE e.createdBy = :createdBy GROUP BY e.status")
    List<Object[]> countExamsByStatusForCreator(@Param("createdBy") Long createdBy);

    /**
     * 查找包含指定题目的考试
     */
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.examQuestions eq WHERE eq.questionId = :questionId")
    List<Exam> findExamsContainingQuestion(@Param("questionId") Long questionId);

    /**
     * 获取考试难度分布统计
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN e.passingScore < 60 THEN 'EASY' " +
           "  WHEN e.passingScore < 80 THEN 'MEDIUM' " +
           "  ELSE 'HARD' " +
           "END as difficulty, COUNT(e) " +
           "FROM Exam e WHERE e.status IN ('PUBLISHED', 'ACTIVE', 'COMPLETED') " +
           "GROUP BY " +
           "CASE " +
           "  WHEN e.passingScore < 60 THEN 'EASY' " +
           "  WHEN e.passingScore < 80 THEN 'MEDIUM' " +
           "  ELSE 'HARD' " +
           "END")
    List<Object[]> getExamDifficultyDistribution();

    /**
     * 查找长时间未更新的草稿考试
     */
    @Query("SELECT e FROM Exam e WHERE e.status = 'DRAFT' AND " +
           "e.updatedAt < :thresholdDate ORDER BY e.updatedAt ASC")
    List<Exam> findStaleDraftExams(@Param("thresholdDate") LocalDateTime thresholdDate);
}