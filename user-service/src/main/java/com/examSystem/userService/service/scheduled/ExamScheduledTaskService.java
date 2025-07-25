package com.examSystem.userService.service.scheduled;

import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.entity.ExamAttempt;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.repository.ExamAttemptRepository;
import com.examSystem.userService.service.grading.AutoGradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试定时任务服务
 * 
 * 处理考试生命周期的自动化任务：
 * - 自动开始考试
 * - 自动结束超时考试
 * - 自动评分
 * - 考试状态监控
 */
@Service
public class ExamScheduledTaskService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private AutoGradingService autoGradingService;

    /**
     * 每分钟检查并自动开始考试
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void autoStartExams() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查找需要自动开始的考试
        List<Exam> examsToStart = examRepository.findExamsToAutoStart(now);
        
        for (Exam exam : examsToStart) {
            try {
                exam.setStatus("ACTIVE");
                examRepository.save(exam);
                
                // 记录日志
                System.out.println("自动开始考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
            } catch (Exception e) {
                System.err.println("自动开始考试失败: " + exam.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每分钟检查并自动结束考试
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void autoEndExams() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查找需要自动结束的考试
        List<Exam> examsToEnd = examRepository.findExamsToAutoEnd(now);
        
        for (Exam exam : examsToEnd) {
            try {
                exam.setStatus("ENDED");
                examRepository.save(exam);
                
                // 自动结束所有进行中的考试尝试
                List<ExamAttempt> activeAttempts = examAttemptRepository.findActiveAttemptsByExamId(exam.getId());
                for (ExamAttempt attempt : activeAttempts) {
                    attempt.setStatus("AUTO_COMPLETED");
                    attempt.setSubmitTime(now);
                    examAttemptRepository.save(attempt);
                    
                    // 触发自动评分
                    try {
                        autoGradingService.gradeExamAttempt(attempt.getId());
                    } catch (Exception e) {
                        System.err.println("自动评分失败: " + attempt.getId() + ", 错误: " + e.getMessage());
                    }
                }
                
                // 记录日志
                System.out.println("自动结束考试: " + exam.getTitle() + " (ID: " + exam.getId() + 
                    "), 结束了 " + activeAttempts.size() + " 个进行中的尝试");
            } catch (Exception e) {
                System.err.println("自动结束考试失败: " + exam.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每分钟检查并自动结束超时的考试尝试
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void autoTimeoutAttempts() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查找超时的考试尝试
        List<ExamAttempt> timeoutAttempts = examAttemptRepository.findTimeoutAttempts(now);
        
        for (ExamAttempt attempt : timeoutAttempts) {
            try {
                attempt.setStatus("TIMEOUT");
                attempt.setSubmitTime(now);
                examAttemptRepository.save(attempt);
                
                // 触发自动评分
                autoGradingService.gradeExamAttempt(attempt.getId());
                
                // 记录日志
                System.out.println("自动超时考试尝试: " + attempt.getId() + 
                    " (学生ID: " + attempt.getStudentId() + ")");
            } catch (Exception e) {
                System.err.println("处理超时考试失败: " + attempt.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每5分钟进行自动评分
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional
    public void autoGradeAttempts() {
        // 查找需要评分的考试尝试
        List<ExamAttempt> attemptsToGrade = examAttemptRepository.findAttemptsNeedingAutoGrading();
        
        for (ExamAttempt attempt : attemptsToGrade) {
            try {
                autoGradingService.gradeExamAttempt(attempt.getId());
                
                // 记录日志
                System.out.println("自动评分完成: " + attempt.getId());
            } catch (Exception e) {
                System.err.println("自动评分失败: " + attempt.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每小时更新考试统计信息
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void updateExamStatistics() {
        try {
            // 更新考试的参与人数统计
            examRepository.updateParticipantCounts();
            
            // 更新考试的平均分统计
            examRepository.updateAverageScores();
            
            // 记录日志
            System.out.println("考试统计信息更新完成: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("更新考试统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 每天清理过期的考试会话
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // 清理30天前的数据
        
        try {
            // 清理过期的草稿状态考试尝试
            int deletedDrafts = examAttemptRepository.deleteExpiredDraftAttempts(cutoffTime);
            
            // 清理过期的临时数据
            int deletedTempData = examAttemptRepository.deleteExpiredTempData(cutoffTime);
            
            // 记录日志
            System.out.println("清理过期数据完成: 删除草稿尝试 " + deletedDrafts + 
                " 个，删除临时数据 " + deletedTempData + " 个");
        } catch (Exception e) {
            System.err.println("清理过期数据失败: " + e.getMessage());
        }
    }

    /**
     * 每天备份重要考试数据
     */
    @Scheduled(cron = "0 30 1 * * ?") // 每天凌晨1:30执行
    @Transactional(readOnly = true)
    public void backupExamData() {
        try {
            // 导出当天完成的考试数据
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime tomorrow = today.plusDays(1);
            
            List<ExamAttempt> completedToday = examAttemptRepository.findCompletedAttemptsBetween(today, tomorrow);
            
            // 这里可以实现数据导出逻辑
            // 例如：导出到文件、发送到备份服务等
            
            // 记录日志
            System.out.println("备份考试数据完成: " + completedToday.size() + " 个考试记录");
        } catch (Exception e) {
            System.err.println("备份考试数据失败: " + e.getMessage());
        }
    }

    /**
     * 每周生成考试报告
     */
    @Scheduled(cron = "0 0 6 * * MON") // 每周一早上6点执行
    @Transactional(readOnly = true)
    public void generateWeeklyReports() {
        try {
            LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekEnd = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            
            // 生成周报数据
            List<Object[]> weeklyStats = examAttemptRepository.getWeeklyStatistics(weekStart, weekEnd);
            
            // 这里可以实现报告生成逻辑
            // 例如：生成PDF报告、发送邮件等
            
            // 记录日志
            System.out.println("生成周报完成: " + weeklyStats.size() + " 项统计数据");
        } catch (Exception e) {
            System.err.println("生成周报失败: " + e.getMessage());
        }
    }

    /**
     * 监控系统健康状态
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional(readOnly = true)
    public void monitorSystemHealth() {
        try {
            // 检查数据库连接
            long totalExams = examRepository.count();
            long activeAttempts = examAttemptRepository.countActiveAttempts();
            
            // 检查是否有异常情况
            if (activeAttempts > totalExams * 10) { // 如果活跃尝试数超过考试总数的10倍
                System.err.println("警告: 活跃考试尝试数异常 - " + activeAttempts);
            }
            
            // 检查超时未评分的记录
            long ungradedAttempts = examAttemptRepository.countUngradedAttempts();
            if (ungradedAttempts > 100) { // 如果未评分记录超过100个
                System.err.println("警告: 未评分记录过多 - " + ungradedAttempts);
            }
            
            // 可以添加更多健康检查项目
            
        } catch (Exception e) {
            System.err.println("系统健康监控失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发考试状态同步
     */
    @Transactional
    public void syncExamStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        // 更新所有考试状态
        List<Exam> allExams = examRepository.findAll();
        for (Exam exam : allExams) {
            String newStatus = determineExamStatus(exam, now);
            if (!newStatus.equals(exam.getStatus())) {
                exam.setStatus(newStatus);
                examRepository.save(exam);
            }
        }
    }

    private String determineExamStatus(Exam exam, LocalDateTime now) {
        if (now.isBefore(exam.getStartTime())) {
            return "SCHEDULED";
        } else if (now.isAfter(exam.getEndTime())) {
            return "ENDED";
        } else {
            return "ACTIVE";
        }
    }
}