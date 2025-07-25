package com.examSystem.userService.repository;

import com.examSystem.userService.entity.ExamAttempt;
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
 * 考试记录数据访问接口
 * 
 * 基于数据库设计文档中的exam_attempts表结构
 * 提供考试记录相关的数据库操作方法，支持成绩统计和分析
 */
@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /**
     * 根据考试码查找考试记录
     */
    Optional<ExamAttempt> findByAttemptCode(String attemptCode);

    /**
     * 检查考试码是否存在
     */
    boolean existsByAttemptCode(String attemptCode);

    /**
     * 根据考试ID查找所有考试记录
     */
    Page<ExamAttempt> findByExamId(Long examId, Pageable pageable);

    /**
     * 根据用户ID查找所有考试记录
     */
    Page<ExamAttempt> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和考试ID查找考试记录
     */
    List<ExamAttempt> findByUserIdAndExamIdOrderByStartedAtDesc(Long userId, Long examId);

    /**
     * 根据用户ID和考试ID查找最新的考试记录
     */
    Optional<ExamAttempt> findFirstByUserIdAndExamIdOrderByStartedAtDesc(Long userId, Long examId);

    /**
     * 根据状态查找考试记录
     */
    Page<ExamAttempt> findByStatus(ExamAttempt.AttemptStatus status, Pageable pageable);

    /**
     * 查找已完成的考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED', 'GRADED', 'REVIEWED') " +
           "ORDER BY ea.submittedAt DESC")
    Page<ExamAttempt> findCompletedAttempts(Pageable pageable);

    /**
     * 查找进行中的考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.status IN ('STARTED', 'IN_PROGRESS') " +
           "ORDER BY ea.startedAt DESC")
    Page<ExamAttempt> findActiveAttempts(Pageable pageable);

    /**
     * 查找需要评分的考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED') " +
           "ORDER BY ea.submittedAt ASC")
    Page<ExamAttempt> findAttemptsNeedingGrading(Pageable pageable);

    /**
     * 查找指定考试的已完成记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.examId = :examId AND " +
           "ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED', 'GRADED', 'REVIEWED') " +
           "ORDER BY ea.submittedAt DESC")
    Page<ExamAttempt> findCompletedAttemptsByExam(@Param("examId") Long examId, Pageable pageable);

    /**
     * 查找指定用户的已完成记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.userId = :userId AND " +
           "ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED', 'GRADED', 'REVIEWED') " +
           "ORDER BY ea.submittedAt DESC")
    Page<ExamAttempt> findCompletedAttemptsByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计用户在指定考试的尝试次数
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.userId = :userId AND ea.examId = :examId")
    int countByUserIdAndExamId(@Param("userId") Long userId, @Param("examId") Long examId);

    /**
     * 统计考试的总尝试次数
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.examId = :examId")
    long countByExamId(@Param("examId") Long examId);

    /**
     * 统计考试的完成次数
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.examId = :examId AND " +
           "ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED', 'GRADED', 'REVIEWED')")
    long countCompletedByExamId(@Param("examId") Long examId);

    /**
     * 统计用户的总考试次数
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 统计用户的完成考试次数
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.userId = :userId AND " +
           "ea.status IN ('SUBMITTED', 'AUTO_SUBMITTED', 'GRADED', 'REVIEWED')")
    long countCompletedByUserId(@Param("userId") Long userId);

    /**
     * 计算考试的平均分
     */
    @Query("SELECT AVG(ea.score) FROM ExamAttempt ea WHERE ea.examId = :examId AND " +
           "ea.status IN ('GRADED', 'REVIEWED') AND ea.score IS NOT NULL")
    BigDecimal getAverageScoreByExamId(@Param("examId") Long examId);

    /**
     * 计算考试的最高分
     */
    @Query("SELECT MAX(ea.score) FROM ExamAttempt ea WHERE ea.examId = :examId AND " +
           "ea.status IN ('GRADED', 'REVIEWED') AND ea.score IS NOT NULL")
    BigDecimal getMaxScoreByExamId(@Param("examId") Long examId);

    /**
     * 计算考试的最低分
     */
    @Query("SELECT MIN(ea.score) FROM ExamAttempt ea WHERE ea.examId = :examId AND " +
           "ea.status IN ('GRADED', 'REVIEWED') AND ea.score IS NOT NULL")
    BigDecimal getMinScoreByExamId(@Param("examId") Long examId);

    /**
     * 获取考试的成绩分布
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN ea.percentage >= 90 THEN 'A' " +
           "  WHEN ea.percentage >= 80 THEN 'B' " +
           "  WHEN ea.percentage >= 70 THEN 'C' " +
           "  WHEN ea.percentage >= 60 THEN 'D' " +
           "  ELSE 'F' " +
           "END as grade, COUNT(ea) " +
           "FROM ExamAttempt ea WHERE ea.examId = :examId AND ea.status IN ('GRADED', 'REVIEWED') " +
           "GROUP BY " +
           "CASE " +
           "  WHEN ea.percentage >= 90 THEN 'A' " +
           "  WHEN ea.percentage >= 80 THEN 'B' " +
           "  WHEN ea.percentage >= 70 THEN 'C' " +
           "  WHEN ea.percentage >= 60 THEN 'D' " +
           "  ELSE 'F' " +
           "END")
    List<Object[]> getGradeDistributionByExamId(@Param("examId") Long examId);

    /**
     * 获取及格率
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN ea.percentage >= :passingScore THEN 1 END) * 100.0 / COUNT(ea) " +
           "FROM ExamAttempt ea WHERE ea.examId = :examId AND ea.status IN ('GRADED', 'REVIEWED')")
    BigDecimal getPassingRateByExamId(@Param("examId") Long examId, @Param("passingScore") BigDecimal passingScore);

    /**
     * 获取考试的详细统计信息
     */
    @Query("SELECT " +
           "COUNT(ea) as totalAttempts, " +
           "COUNT(CASE WHEN ea.status IN ('GRADED', 'REVIEWED') THEN 1 END) as gradedAttempts, " +
           "AVG(ea.score) as avgScore, " +
           "MAX(ea.score) as maxScore, " +
           "MIN(ea.score) as minScore, " +
           "AVG(ea.percentage) as avgPercentage " +
           "FROM ExamAttempt ea WHERE ea.examId = :examId")
    List<Object[]> getExamStatistics(@Param("examId") Long examId);

    /**
     * 获取用户的考试统计信息
     */
    @Query("SELECT " +
           "COUNT(ea) as totalAttempts, " +
           "COUNT(CASE WHEN ea.status IN ('GRADED', 'REVIEWED') THEN 1 END) as completedAttempts, " +
           "AVG(ea.score) as avgScore, " +
           "MAX(ea.score) as bestScore " +
           "FROM ExamAttempt ea WHERE ea.userId = :userId")
    List<Object[]> getUserStatistics(@Param("userId") Long userId);

    /**
     * 查找超时的考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.status IN ('STARTED', 'IN_PROGRESS') AND " +
           "ea.startedAt < :timeoutThreshold")
    List<ExamAttempt> findTimedOutAttempts(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 查找可疑的考试记录（基于违规次数）
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.violationsCount > :threshold " +
           "ORDER BY ea.violationsCount DESC")
    Page<ExamAttempt> findSuspiciousAttempts(@Param("threshold") Integer threshold, Pageable pageable);

    /**
     * 查找监考评分低的记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.proctoringScore IS NOT NULL AND ea.proctoringScore < :threshold " +
           "ORDER BY ea.proctoringScore ASC")
    Page<ExamAttempt> findLowProctoringScoreAttempts(@Param("threshold") BigDecimal threshold, Pageable pageable);

    /**
     * 根据时间范围查找考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE " +
           "(:startDate IS NULL OR ea.startedAt >= :startDate) AND " +
           "(:endDate IS NULL OR ea.startedAt <= :endDate)")
    Page<ExamAttempt> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    Pageable pageable);

    /**
     * 更新考试记录状态
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.status = :status WHERE ea.id = :attemptId")
    void updateStatus(@Param("attemptId") Long attemptId, @Param("status") ExamAttempt.AttemptStatus status);

    /**
     * 提交考试
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.status = 'SUBMITTED', ea.submittedAt = :submittedAt " +
           "WHERE ea.id = :attemptId")
    void submitAttempt(@Param("attemptId") Long attemptId, @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 自动提交考试
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.status = 'AUTO_SUBMITTED', ea.submittedAt = :submittedAt " +
           "WHERE ea.id = :attemptId")
    void autoSubmitAttempt(@Param("attemptId") Long attemptId, @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 更新考试成绩
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET " +
           "ea.status = 'GRADED', " +
           "ea.score = :score, " +
           "ea.percentage = :percentage, " +
           "ea.grade = :grade, " +
           "ea.pointsEarned = :pointsEarned, " +
           "ea.pointsPossible = :pointsPossible, " +
           "ea.gradedAt = :gradedAt " +
           "WHERE ea.id = :attemptId")
    void updateGrade(@Param("attemptId") Long attemptId,
                   @Param("score") BigDecimal score,
                   @Param("percentage") BigDecimal percentage,
                   @Param("grade") String grade,
                   @Param("pointsEarned") BigDecimal pointsEarned,
                   @Param("pointsPossible") BigDecimal pointsPossible,
                   @Param("gradedAt") LocalDateTime gradedAt);

    /**
     * 更新剩余时间
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.remainingTime = :remainingTime, ea.lastActivity = :lastActivity " +
           "WHERE ea.id = :attemptId")
    void updateRemainingTime(@Param("attemptId") Long attemptId, 
                           @Param("remainingTime") Integer remainingTime,
                           @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * 增加违规次数
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.violationsCount = ea.violationsCount + 1 WHERE ea.id = :attemptId")
    void incrementViolationsCount(@Param("attemptId") Long attemptId);

    /**
     * 更新监考评分
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.proctoringScore = :proctoringScore WHERE ea.id = :attemptId")
    void updateProctoringScore(@Param("attemptId") Long attemptId, @Param("proctoringScore") BigDecimal proctoringScore);

    /**
     * 批量自动提交超时的考试
     */
    @Modifying
    @Query("UPDATE ExamAttempt ea SET ea.status = 'AUTO_SUBMITTED', ea.submittedAt = :submittedAt " +
           "WHERE ea.status IN ('STARTED', 'IN_PROGRESS') AND ea.startedAt < :timeoutThreshold")
    int batchAutoSubmitTimedOut(@Param("timeoutThreshold") LocalDateTime timeoutThreshold,
                              @Param("submittedAt") LocalDateTime submittedAt);

    /**
     * 获取指定时间段的考试参与统计
     */
    @Query("SELECT DATE(ea.startedAt) as examDate, COUNT(ea) as attemptCount " +
           "FROM ExamAttempt ea WHERE ea.startedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(ea.startedAt) ORDER BY examDate")
    List<Object[]> getAttemptCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 获取热门考试排行（按参与人数）
     */
    @Query("SELECT ea.examId, COUNT(ea) as attemptCount FROM ExamAttempt ea " +
           "GROUP BY ea.examId ORDER BY attemptCount DESC")
    Page<Object[]> getPopularExamsRanking(Pageable pageable);

    /**
     * 获取用户最近的考试记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.userId = :userId " +
           "ORDER BY ea.startedAt DESC")
    Page<ExamAttempt> findRecentAttemptsByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取考试的最佳成绩记录
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.examId = :examId AND ea.status IN ('GRADED', 'REVIEWED') " +
           "ORDER BY ea.score DESC")
    Page<ExamAttempt> findTopScoresByExam(@Param("examId") Long examId, Pageable pageable);

    /**
     * 查找用户在指定考试的最佳成绩
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.userId = :userId AND ea.examId = :examId AND " +
           "ea.status IN ('GRADED', 'REVIEWED') ORDER BY ea.score DESC")
    Optional<ExamAttempt> findBestScoreByUserAndExam(@Param("userId") Long userId, @Param("examId") Long examId);

    /**
     * 统计用户的及格考试数量
     */
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.userId = :userId AND " +
           "ea.status IN ('GRADED', 'REVIEWED') AND ea.percentage >= :passingScore")
    long countPassedExamsByUser(@Param("userId") Long userId, @Param("passingScore") BigDecimal passingScore);
}