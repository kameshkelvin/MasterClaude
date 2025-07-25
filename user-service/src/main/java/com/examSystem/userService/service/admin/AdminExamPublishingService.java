package com.examSystem.userService.service.admin;

import com.examSystem.userService.entity.Exam;
import com.examSystem.userService.repository.ExamRepository;
import com.examSystem.userService.repository.ExamQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 管理员考试发布和调度服务
 * 
 * 提供考试发布、调度管理、自动状态更新等功能
 */
@Service
@Transactional
public class AdminExamPublishingService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    /**
     * 发布考试
     */
    public void publishExam(Long examId, LocalDateTime publishTime) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        // 验证考试是否可以发布
        validateExamForPublish(exam);

        // 设置发布时间
        LocalDateTime actualPublishTime = publishTime != null ? publishTime : LocalDateTime.now();
        
        // 更新考试状态
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        exam.setPublishedAt(actualPublishTime);
        
        examRepository.save(exam);
    }

    /**
     * 批量发布考试
     */
    public void batchPublishExams(List<Long> examIds, LocalDateTime publishTime) {
        LocalDateTime actualPublishTime = publishTime != null ? publishTime : LocalDateTime.now();
        
        for (Long examId : examIds) {
            try {
                publishExam(examId, actualPublishTime);
            } catch (Exception e) {
                // 记录错误但继续处理其他考试
                System.err.println("发布考试失败 ID: " + examId + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 调度考试（设置可用时间）
     */
    public void scheduleExam(Long examId, LocalDateTime availableFrom, LocalDateTime availableUntil) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        // 验证时间设置
        validateScheduleTime(availableFrom, availableUntil);

        exam.setAvailableFrom(availableFrom);
        exam.setAvailableUntil(availableUntil);

        // 如果考试已发布且开始时间已到，设置为活跃状态
        if (Exam.ExamStatus.PUBLISHED.equals(exam.getStatus()) && 
            availableFrom != null && !availableFrom.isAfter(LocalDateTime.now())) {
            exam.setStatus(Exam.ExamStatus.ACTIVE);
        }

        examRepository.save(exam);
    }

    /**
     * 立即发布并激活考试
     */
    public void publishAndActivateExam(Long examId) {
        publishExam(examId, LocalDateTime.now());
        activateExam(examId);
    }

    /**
     * 激活考试
     */
    public void activateExam(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.PUBLISHED.equals(exam.getStatus())) {
            throw new RuntimeException("只有已发布的考试才能激活");
        }

        // 检查时间限制
        LocalDateTime now = LocalDateTime.now();
        if (exam.getAvailableFrom() != null && exam.getAvailableFrom().isAfter(now)) {
            throw new RuntimeException("考试尚未到开始时间");
        }
        if (exam.getAvailableUntil() != null && exam.getAvailableUntil().isBefore(now)) {
            throw new RuntimeException("考试已过结束时间");
        }

        exam.setStatus(Exam.ExamStatus.ACTIVE);
        examRepository.save(exam);
    }

    /**
     * 暂停考试
     */
    public void pauseExam(Long examId, String reason) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.ACTIVE.equals(exam.getStatus())) {
            throw new RuntimeException("只有活跃状态的考试才能暂停");
        }

        exam.setStatus(Exam.ExamStatus.PAUSED);
        // 可以在exam的metadata字段中记录暂停原因
        exam.setMetadata("{\"pauseReason\":\"" + reason + "\",\"pausedAt\":\"" + LocalDateTime.now() + "\"}");
        
        examRepository.save(exam);
    }

    /**
     * 恢复暂停的考试
     */
    public void resumeExam(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.PAUSED.equals(exam.getStatus())) {
            throw new RuntimeException("只有暂停状态的考试才能恢复");
        }

        // 检查时间限制
        LocalDateTime now = LocalDateTime.now();
        if (exam.getAvailableUntil() != null && exam.getAvailableUntil().isBefore(now)) {
            throw new RuntimeException("考试已过结束时间，无法恢复");
        }

        exam.setStatus(Exam.ExamStatus.ACTIVE);
        examRepository.save(exam);
    }

    /**
     * 结束考试
     */
    public void endExam(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.ACTIVE.equals(exam.getStatus()) && 
            !Exam.ExamStatus.PAUSED.equals(exam.getStatus())) {
            throw new RuntimeException("只有活跃或暂停状态的考试才能结束");
        }

        exam.setStatus(Exam.ExamStatus.COMPLETED);
        exam.setCompletedAt(LocalDateTime.now());
        
        examRepository.save(exam);
    }

    /**
     * 延长考试时间
     */
    public void extendExamTime(Long examId, LocalDateTime newEndTime) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.ACTIVE.equals(exam.getStatus()) && 
            !Exam.ExamStatus.PUBLISHED.equals(exam.getStatus())) {
            throw new RuntimeException("只有活跃或已发布的考试才能延长时间");
        }

        if (newEndTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("新的结束时间不能早于当前时间");
        }

        if (exam.getAvailableUntil() != null && newEndTime.isBefore(exam.getAvailableUntil())) {
            throw new RuntimeException("新的结束时间不能早于原结束时间");
        }

        exam.setAvailableUntil(newEndTime);
        examRepository.save(exam);
    }

    /**
     * 撤销发布
     */
    public void unpublishExam(Long examId, String reason) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));

        if (!Exam.ExamStatus.PUBLISHED.equals(exam.getStatus())) {
            throw new RuntimeException("只有已发布的考试才能撤销发布");
        }

        // 检查是否有学生已经开始考试
        if (exam.getAttemptsCount() != null && exam.getAttemptsCount() > 0) {
            throw new RuntimeException("已有学生开始考试，无法撤销发布");
        }

        exam.setStatus(Exam.ExamStatus.DRAFT);
        exam.setPublishedAt(null);
        // 记录撤销原因
        exam.setMetadata("{\"unpublishReason\":\"" + reason + "\",\"unpublishedAt\":\"" + LocalDateTime.now() + "\"}");
        
        examRepository.save(exam);
    }

    /**
     * 获取即将开始的考试
     */
    @Transactional(readOnly = true)
    public List<Exam> getUpcomingExams(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcomingTime = now.plusHours(hours);
        return examRepository.findUpcomingExams(now, upcomingTime);
    }

    /**
     * 获取已过期的考试
     */
    @Transactional(readOnly = true)
    public List<Exam> getExpiredExams() {
        return examRepository.findExpiredExams(LocalDateTime.now());
    }

    /**
     * 获取需要自动结束的考试
     */
    @Transactional(readOnly = true)
    public List<Exam> getExamsToAutoEnd() {
        return examRepository.findExamsToAutoEnd(LocalDateTime.now());
    }

    /**
     * 获取可以开始的考试
     */
    @Transactional(readOnly = true)
    public List<Exam> getExamsToStart() {
        return examRepository.findExamsToStart(LocalDateTime.now());
    }

    /**
     * 自动开始考试（定时任务）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Async
    public void autoStartExams() {
        List<Exam> examsToStart = getExamsToStart();
        for (Exam exam : examsToStart) {
            try {
                activateExam(exam.getId());
                System.out.println("自动开始考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
            } catch (Exception e) {
                System.err.println("自动开始考试失败 ID: " + exam.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 自动结束考试（定时任务）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Async
    public void autoEndExams() {
        List<Exam> examsToEnd = getExamsToAutoEnd();
        for (Exam exam : examsToEnd) {
            try {
                endExam(exam.getId());
                System.out.println("自动结束考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
            } catch (Exception e) {
                System.err.println("自动结束考试失败 ID: " + exam.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 检查并处理过期考试（定时任务）
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Async
    public void processExpiredExams() {
        List<Exam> expiredExams = getExpiredExams();
        for (Exam exam : expiredExams) {
            try {
                if (Exam.ExamStatus.PUBLISHED.equals(exam.getStatus()) || 
                    Exam.ExamStatus.ACTIVE.equals(exam.getStatus())) {
                    endExam(exam.getId());
                    System.out.println("处理过期考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
                }
            } catch (Exception e) {
                System.err.println("处理过期考试失败 ID: " + exam.getId() + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 获取考试发布统计
     */
    @Transactional(readOnly = true)
    public ExamPublishingStatistics getPublishingStatistics() {
        List<Object[]> statusStats = examRepository.countExamsByStatus();
        
        long draftCount = 0;
        long reviewCount = 0;
        long publishedCount = 0;
        long activeCount = 0;
        long completedCount = 0;
        long pausedCount = 0;
        
        for (Object[] stat : statusStats) {
            String status = (String) stat[0];
            long count = ((Number) stat[1]).longValue();
            
            switch (status) {
                case "DRAFT":
                    draftCount = count;
                    break;
                case "REVIEW":
                    reviewCount = count;
                    break;
                case "PUBLISHED":
                    publishedCount = count;
                    break;
                case "ACTIVE":
                    activeCount = count;
                    break;
                case "COMPLETED":
                    completedCount = count;
                    break;
                case "PAUSED":
                    pausedCount = count;
                    break;
            }
        }
        
        return new ExamPublishingStatistics(
            draftCount, reviewCount, publishedCount, 
            activeCount, completedCount, pausedCount,
            getUpcomingExams(24).size(),
            getExpiredExams().size()
        );
    }

    // 私有辅助方法

    /**
     * 验证考试是否可以发布
     */
    private void validateExamForPublish(Exam exam) {
        if (!Exam.ExamStatus.DRAFT.equals(exam.getStatus()) && 
            !Exam.ExamStatus.REVIEW.equals(exam.getStatus())) {
            throw new RuntimeException("只有草稿或审核状态的考试才能发布");
        }
        
        if (exam.getQuestionsCount() == null || exam.getQuestionsCount() == 0) {
            throw new RuntimeException("考试必须包含至少一道题目才能发布");
        }
        
        if (exam.getTotalPoints() == null || exam.getTotalPoints().doubleValue() <= 0) {
            throw new RuntimeException("考试总分必须大于0才能发布");
        }
        
        if (exam.getDurationMinutes() == null || exam.getDurationMinutes() <= 0) {
            throw new RuntimeException("考试时长必须设置才能发布");
        }
        
        if (exam.getPassingScore() == null) {
            throw new RuntimeException("及格分数必须设置才能发布");
        }
        
        if (exam.getTitle() == null || exam.getTitle().trim().isEmpty()) {
            throw new RuntimeException("考试标题不能为空");
        }
    }

    /**
     * 验证调度时间设置
     */
    private void validateScheduleTime(LocalDateTime availableFrom, LocalDateTime availableUntil) {
        if (availableFrom != null && availableUntil != null) {
            if (!availableFrom.isBefore(availableUntil)) {
                throw new RuntimeException("开始时间必须早于结束时间");
            }
        }
        
        if (availableUntil != null && availableUntil.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("结束时间不能早于当前时间");
        }
    }

    /**
     * 考试发布统计信息内部类
     */
    public static class ExamPublishingStatistics {
        private final long draftCount;
        private final long reviewCount;
        private final long publishedCount;
        private final long activeCount;
        private final long completedCount;
        private final long pausedCount;
        private final long upcomingCount;
        private final long expiredCount;

        public ExamPublishingStatistics(long draftCount, long reviewCount, long publishedCount,
                                      long activeCount, long completedCount, long pausedCount,
                                      long upcomingCount, long expiredCount) {
            this.draftCount = draftCount;
            this.reviewCount = reviewCount;
            this.publishedCount = publishedCount;
            this.activeCount = activeCount;
            this.completedCount = completedCount;
            this.pausedCount = pausedCount;
            this.upcomingCount = upcomingCount;
            this.expiredCount = expiredCount;
        }

        // Getters
        public long getDraftCount() { return draftCount; }
        public long getReviewCount() { return reviewCount; }
        public long getPublishedCount() { return publishedCount; }
        public long getActiveCount() { return activeCount; }
        public long getCompletedCount() { return completedCount; }
        public long getPausedCount() { return pausedCount; }
        public long getUpcomingCount() { return upcomingCount; }
        public long getExpiredCount() { return expiredCount; }
        public long getTotalCount() { 
            return draftCount + reviewCount + publishedCount + activeCount + completedCount + pausedCount; 
        }
    }
}