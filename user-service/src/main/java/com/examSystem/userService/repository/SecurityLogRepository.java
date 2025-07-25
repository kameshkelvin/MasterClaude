package com.examSystem.userService.repository;

import com.examSystem.userService.entity.SecurityLog;
import com.examSystem.userService.service.security.ExamSecurityService.SecurityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全日志数据访问层
 */
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    /**
     * 根据学生ID和考试尝试ID查找安全日志
     */
    List<SecurityLog> findByStudentIdAndAttemptIdOrderByCreatedAtDesc(Long studentId, Long attemptId);

    /**
     * 根据考试尝试ID查找安全日志
     */
    List<SecurityLog> findByAttemptIdOrderByCreatedAtDesc(Long attemptId);

    /**
     * 查找指定时间范围内的安全违规记录
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE sl.studentId = :studentId AND sl.attemptId = :attemptId " +
           "AND sl.securityLevel IN ('WARNING', 'CRITICAL') ORDER BY sl.createdAt DESC")
    List<SecurityLog> findViolationsByStudentAndAttempt(@Param("studentId") Long studentId, 
                                                       @Param("attemptId") Long attemptId);

    /**
     * 根据安全级别查找日志
     */
    List<SecurityLog> findBySecurityLevelOrderByCreatedAtDesc(SecurityLevel securityLevel);

    /**
     * 查找指定时间范围内的日志
     */
    List<SecurityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据事件类型查找日志
     */
    List<SecurityLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * 分页查找未解决的安全事件
     */
    Page<SecurityLog> findByResolvedAtIsNullAndSecurityLevelInOrderByCreatedAtDesc(
        List<SecurityLevel> securityLevels, Pageable pageable);

    /**
     * 统计学生的违规次数
     */
    @Query("SELECT COUNT(sl) FROM SecurityLog sl WHERE sl.studentId = :studentId " +
           "AND sl.securityLevel IN ('WARNING', 'CRITICAL') " +
           "AND sl.createdAt >= :since")
    long countViolationsByStudentSince(@Param("studentId") Long studentId, 
                                      @Param("since") LocalDateTime since);

    /**
     * 统计考试的安全事件数量
     */
    @Query("SELECT sl.securityLevel, COUNT(sl) FROM SecurityLog sl WHERE sl.attemptId IN " +
           "(SELECT ea.id FROM ExamAttempt ea WHERE ea.examId = :examId) " +
           "GROUP BY sl.securityLevel")
    List<Object[]> countSecurityEventsByExam(@Param("examId") Long examId);

    /**
     * 查找学生最近的IP地址记录
     */
    @Query("SELECT DISTINCT sl.clientIP FROM SecurityLog sl WHERE sl.studentId = :studentId " +
           "AND sl.attemptId = :attemptId AND sl.clientIP IS NOT NULL " +
           "AND sl.createdAt >= :since ORDER BY sl.createdAt DESC")
    List<String> findRecentIPsByStudentAndAttempt(@Param("studentId") Long studentId,
                                                 @Param("attemptId") Long attemptId,
                                                 @Param("since") LocalDateTime since);

    /**
     * 查找高风险学生列表
     */
    @Query("SELECT sl.studentId, COUNT(sl) as violationCount FROM SecurityLog sl " +
           "WHERE sl.securityLevel = 'CRITICAL' AND sl.createdAt >= :since " +
           "GROUP BY sl.studentId HAVING COUNT(sl) >= :threshold " +
           "ORDER BY violationCount DESC")
    List<Object[]> findHighRiskStudents(@Param("since") LocalDateTime since, 
                                       @Param("threshold") long threshold);

    /**
     * 查找异常活跃的IP地址
     */
    @Query("SELECT sl.clientIP, COUNT(DISTINCT sl.studentId) as studentCount, COUNT(sl) as eventCount " +
           "FROM SecurityLog sl WHERE sl.clientIP IS NOT NULL AND sl.createdAt >= :since " +
           "GROUP BY sl.clientIP HAVING COUNT(DISTINCT sl.studentId) >= :studentThreshold " +
           "OR COUNT(sl) >= :eventThreshold ORDER BY eventCount DESC")
    List<Object[]> findSuspiciousIPs(@Param("since") LocalDateTime since,
                                    @Param("studentThreshold") long studentThreshold,
                                    @Param("eventThreshold") long eventThreshold);

    /**
     * 获取安全趋势数据
     */
    @Query("SELECT DATE(sl.createdAt) as date, sl.securityLevel, COUNT(sl) as count " +
           "FROM SecurityLog sl WHERE sl.createdAt >= :since " +
           "GROUP BY DATE(sl.createdAt), sl.securityLevel " +
           "ORDER BY date DESC, sl.securityLevel")
    List<Object[]> getSecurityTrends(@Param("since") LocalDateTime since);

    /**
     * 删除过期的安全日志
     */
    @Query("DELETE FROM SecurityLog sl WHERE sl.createdAt < :cutoffDate " +
           "AND sl.securityLevel = 'INFO' AND sl.resolvedAt IS NOT NULL")
    void deleteExpiredInfoLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 查找需要关注的安全模式
     */
    @Query("SELECT sl.eventType, COUNT(sl) as frequency, " +
           "COUNT(DISTINCT sl.studentId) as affectedStudents, " +
           "MAX(sl.createdAt) as lastOccurrence " +
           "FROM SecurityLog sl WHERE sl.createdAt >= :since " +
           "AND sl.securityLevel IN ('WARNING', 'CRITICAL') " +
           "GROUP BY sl.eventType HAVING COUNT(sl) >= :threshold " +
           "ORDER BY frequency DESC")
    List<Object[]> findSecurityPatterns(@Param("since") LocalDateTime since,
                                       @Param("threshold") long threshold);

    /**
     * 获取实时安全仪表板数据
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN sl.securityLevel = 'CRITICAL' THEN 1 END) as critical, " +
           "COUNT(CASE WHEN sl.securityLevel = 'WARNING' THEN 1 END) as warning, " +
           "COUNT(CASE WHEN sl.securityLevel = 'INFO' THEN 1 END) as info, " +
           "COUNT(DISTINCT sl.studentId) as affectedStudents, " +
           "COUNT(DISTINCT sl.attemptId) as affectedAttempts " +
           "FROM SecurityLog sl WHERE sl.createdAt >= :since")
    Object[] getSecurityDashboardData(@Param("since") LocalDateTime since);
}